package tk.jomp16.habbo.game.item

data class WiredData(
        val id: Int,
        var delay: Int, // delay
        var items: List<Int>, // items
        var message: String, // text box
        var options: List<Int>, // options
        var extradata: String  // extra data
)