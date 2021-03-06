package org.quibot.plugins

import org.quibot._

trait QuiBotPlugin {
	val commands = new collection.mutable.ArrayBuffer[MessageHandler]
	private var bot: Option[QuiBot] = None //injected later
	
	def setBot(bot: QuiBot)	= this.bot = Some(bot)

	def respondTo(regexStr: String)(f: MatchedMessage => Unit) {
        val regex = ("(?i)"+regexStr).r //adding case insensitive
        commands += MessageHandler(regex)(f)
    }

    def say(channel: Channel, msgs: List[String]) : Unit = say(channel, msgs:_*)
    def say(channel: Channel, msgs: String*) = bot map { quibot => quibot say (channel, msgs:_*) }
    def sayTo(channel: Channel, userNick: String, msgs: List[String]) : Unit = sayTo(channel, userNick, msgs:_*)
    def sayTo(channel: Channel, userNick: String, msgs: String*) = bot map { quibot =>  quibot sayTo(channel, userNick, msgs:_*) }
    def sayAllChannels(msgs: String*) = bot map { quibot => 
	    quibot.joinedChannels.foreach { c => quibot say(c, msgs:_*) } 
	}
	def reply(msg: MatchedMessage, replies: List[String]) : Unit = reply(msg, replies:_*)
	def reply(msg: MatchedMessage, replies: String*) = bot map { quibot =>
		sayTo(msg.channel, msg.user.nick.nickname, replies:_*)
	}
}