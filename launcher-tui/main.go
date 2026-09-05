/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/charmbracelet/bubbles/textinput"
	"github.com/charmbracelet/bubbles/viewport"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type lineMsg struct {
	conn net.Conn
	line string
}
type shutdownDoneMsg struct{}
type processMsg struct{ cmd *exec.Cmd }

type model struct {
	jarPath            string
	socketPath         string
	listener           net.Listener
	control            net.Conn
	process            *exec.Cmd
	shuttingDown       bool
	shutdownRequested  bool
	input              textinput.Model
	logViewport        viewport.Model
	logs               []string
	users, roomsLoaded int
	width, height      int
}

var (
	accent = lipgloss.NewStyle().Foreground(lipgloss.Color("#F7C948"))
	muted  = lipgloss.NewStyle().Foreground(lipgloss.Color("#7D8590"))
	good   = lipgloss.NewStyle().Foreground(lipgloss.Color("#56D364"))
	panel  = lipgloss.NewStyle().Border(lipgloss.RoundedBorder()).BorderForeground(lipgloss.Color("#30363D")).Padding(0, 1)
)

func main() {
	jar := "launcher.jar"
	if len(os.Args) > 1 {
		jar = os.Args[1]
	}
	socket := filepath.Join(os.TempDir(), "habbo-r63b.sock")
	_ = os.Remove(socket)
	listener, err := net.Listen("unix", socket)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	defer func() { listener.Close(); os.Remove(socket) }()

	in := textinput.New()
	in.Prompt = "> "
	in.Placeholder = "admin command (future)"
	in.CharLimit = 200
	in.Width = 60
	in.Focus()
	m := model{
		jarPath: jar, socketPath: socket, listener: listener, input: in,
		logViewport: viewport.New(80, 10),
	}
	p := tea.NewProgram(m, tea.WithAltScreen())
	go acceptConnections(listener, p)
	if _, err := p.Run(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func acceptConnections(listener net.Listener, p *tea.Program) {
	for {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		go readConnection(conn, p)
	}
}

func readConnection(conn net.Conn, p *tea.Program) {
	scanner := bufio.NewScanner(conn)
	for scanner.Scan() {
		line := scanner.Text()
		p.Send(lineMsg{conn: conn, line: line})
	}
	conn.Close()
}

func (m model) Init() tea.Cmd {
	return func() tea.Msg {
		cmd := exec.Command("java", "-Dhabbo.tui.socket="+m.socketPath, "-jar", m.jarPath)
		cmd.Stdout = nil
		cmd.Stderr = nil
		if err := cmd.Start(); err != nil {
			return lineMsg{line: "LOG\tERROR\tlauncher: " + err.Error()}
		}
		return processMsg{cmd: cmd}
	}
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case processMsg:
		m.process = msg.cmd
		m.logs = append(m.logs, colorLog("INFO", fmt.Sprintf("[INFO] server started (pid %d)", msg.cmd.Process.Pid)))
		m.refreshLogs()
		if m.shutdownRequested {
			return m, m.shutdown()
		}
	case shutdownDoneMsg:
		if m.control != nil {
			_ = m.control.Close()
		}
		return m, tea.Quit
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		m.input.Width = max(20, msg.Width-4)
		m.logViewport.Width = max(20, msg.Width-32)
		m.logViewport.Height = max(5, msg.Height-10)
		m.refreshLogs()
	case lineMsg:
		if strings.HasPrefix(msg.line, "HELLO\tCONTROL") {
			m.control = msg.conn
		}
		m.handleLine(msg.line)
	case tea.KeyMsg:
		if m.shuttingDown {
			return m, nil
		}
		switch msg.String() {
		case "left", "ctrl+left", "alt+left":
			m.logViewport.ScrollLeft(8)
			return m, nil
		case "right", "ctrl+right", "alt+right":
			m.logViewport.ScrollRight(8)
			return m, nil
		}
		if msg.String() == "ctrl+c" || msg.String() == "esc" {
			m.shuttingDown = true
			m.shutdownRequested = true
			m.logs = append(m.logs, colorLog("INFO", "[INFO] Graceful shutdown requested; waiting for server..."))
			m.refreshLogs()
			if m.process != nil {
				return m, m.shutdown()
			}
			return m, nil
		}
		if msg.String() == "enter" && strings.TrimSpace(m.input.Value()) != "" {
			command := strings.TrimSpace(m.input.Value())
			m.input.Reset()
			if m.control != nil {
				fmt.Fprintf(m.control, "CMD\t%s\n", command)
			}
			m.logs = append(m.logs, "> "+command)
			return m, nil
		}
		var viewportCmd tea.Cmd
		m.logViewport, viewportCmd = m.logViewport.Update(msg)
		if viewportCmd != nil {
			return m, viewportCmd
		}
		var cmd tea.Cmd
		m.input, cmd = m.input.Update(msg)
		return m, cmd
	}
	return m, nil
}

func (m model) shutdown() tea.Cmd {
	if m.process == nil || m.process.Process == nil {
		return func() tea.Msg { return shutdownDoneMsg{} }
	}

	process := m.process
	return func() tea.Msg {
		// SIGTERM lets the JVM execute its registered shutdown hook gracefully.
		_ = process.Process.Signal(syscall.SIGTERM)
		done := make(chan struct{})
		go func() {
			_, _ = process.Process.Wait()
			close(done)
		}()
		select {
		case <-done:
		case <-time.After(time.Minute):
			_ = process.Process.Kill()
			<-done
		}
		return shutdownDoneMsg{}
	}
}

func (m *model) handleLine(line string) {
	parts := strings.SplitN(line, "\t", 5)
	if len(parts) == 0 {
		return
	}
	switch parts[0] {
	case "HELLO":
		if len(parts) > 1 && parts[1] == "CONTROL" && m.control == nil { /* connectionMsg assigns it */
		}
	case "STATS":
		for _, value := range strings.Split(line, "\t")[1:] {
			pair := strings.SplitN(value, "=", 2)
			if len(pair) != 2 {
				continue
			}
			n, _ := strconv.Atoi(pair[1])
			switch pair[0] {
			case "users":
				m.users = n
			case "rooms_loaded":
				m.roomsLoaded = n
			}
		}
	case "LOG":
		if len(parts) == 5 {
			message := strings.ReplaceAll(parts[4], `\n`, "\n")
			m.logs = append(m.logs, colorLog(parts[1], parts[2]+" ["+parts[1]+"] ("+parts[3]+") "+message))
		} else if len(parts) == 3 {
			message := strings.ReplaceAll(parts[2], `\n`, "\n")
			m.logs = append(m.logs, colorLog(parts[1], "["+parts[1]+"] "+message))
		}
	case "RESULT":
		if len(parts) == 3 {
			m.logs = append(m.logs, "[command] "+parts[2])
		}
	}
	if len(m.logs) > 200 {
		m.logs = m.logs[len(m.logs)-200:]
	}
	m.refreshLogs()
}

func (m *model) refreshLogs() {
	atBottom := m.logViewport.AtBottom()
	m.logViewport.SetContent(strings.Join(m.logs, "\n"))
	if atBottom {
		m.logViewport.GotoBottom()
	}
}

func colorLog(level, message string) string {
	var style lipgloss.Style
	switch strings.ToUpper(level) {
	case "TRACE":
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#6E7681"))
	case "DEBUG":
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#58A6FF"))
	case "INFO":
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#56D364"))
	case "WARN":
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#F7C948"))
	case "ERROR", "FATAL":
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#FF7B72"))
	default:
		style = lipgloss.NewStyle().Foreground(lipgloss.Color("#C9D1D9"))
	}
	return style.Render(message)
}

func (m model) logView() string {
	content := strings.Split(m.logViewport.View(), "\n")
	barHeight := max(5, m.logViewport.Height)
	thumb := int(m.logViewport.ScrollPercent() * float64(barHeight-1))
	for len(content) < barHeight {
		content = append(content, "")
	}
	for i := 0; i < barHeight && i < len(content); i++ {
		bar := muted.Render("|")
		if i == thumb {
			bar = accent.Render("#")
		}
		content[i] = content[i] + " " + bar
	}

	trackWidth := max(20, m.logViewport.Width)
	horizontalThumb := int(m.logViewport.HorizontalScrollPercent() * float64(trackWidth-1))
	horizontal := muted.Render(strings.Repeat("-", horizontalThumb)) +
		accent.Render("#") +
		muted.Render(strings.Repeat("-", trackWidth-horizontalThumb-1))
	content = append(content[:barHeight], horizontal)
	return strings.Join(content, "\n")
}

func (m model) View() string {
	if m.width == 0 {
		return "Starting Habbo Launcher..."
	}
	title := accent.Render("HABBO HOTEL") + muted.Render("  //  launcher")
	status := good.Render("● online")
	metrics := panel.Render(fmt.Sprintf("%s\nusers       %d\nrooms loaded %d", accent.Render("LIVE METRICS"), m.users, m.roomsLoaded))
	logHeight := max(5, m.height-10)
	logPanel := panel.Width(max(20, m.width-28)).Height(logHeight + 1).Render(m.logView())
	return fmt.Sprintf("%s    %s\n\n%s\n\n%s\n%s", title, status, lipgloss.JoinHorizontal(lipgloss.Top, logPanel, metrics), m.input.View(), muted.Render("ctrl+←/→ or alt+←/→: log scroll  |  ctrl+c / esc: shutdown"))
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}
