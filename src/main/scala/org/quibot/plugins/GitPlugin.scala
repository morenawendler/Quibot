package org.quibot.plugins

import org.quibot._
import java.io.File

case class GitPlugin(gitRepositoryDir: String) extends QuiBotPlugin with CLICommands {
    currDir = Some(new File(gitRepositoryDir))

    val timer = new java.util.Timer()
    timer schedule ( new java.util.TimerTask {
        override def run() = fetchGit
    }, 60000, 150000) // 5 min
    
    respondTo("git +log *(.*)$") { msg =>
        println("[INFO] 'git log' "+msg) 
        val branch = if (msg.groups.size > 0 && msg.groups(0) != "") msg.groups(0) else "origin/master"
        val (_, content) = exec("git log --oneline -n 5 "+branch)
        say(msg.channel, "[branch "+branch+"]")
        say(msg.channel, content map ( commit => "    " + commit ) toArray :_*)
    }

    def getCommitMessages(lowerCommit: String, upperCommit: String, branch: String, commitLimit: Int = 10) = {
        val (_, lines) = exec("git log "+lowerCommit+"..."+upperCommit+" --oneline")
        if (lines.size > commitLimit) 
            (lines.take(commitLimit)) ::: List("... (more commits)")
        else
            lines
    }
    def truncateCommitMsg(msg: String, limit: Int = 100) = if (msg.length > limit) msg.take(100) + "..."  else msg

    def fetchGit = {
        val lines = exec("git fetch")._2
        println("[INFO] "+new java.util.Date + " 'git fetch' "+lines.size + " lines of result")
        val regex = """(([a-z0-9]+)\.\.([a-z0-9]+))? +([^ ]+) +-> ([^ ]+).*$""".r
        var messages = List[String]()
        if (lines.size != 0) {
            for (line <- lines) {
                val matchIterator = regex findAllIn line
                val subgroups = matchIterator.matchData.flatMap( m => m.subgroups).toList
                if (subgroups.size > 0) {
                    line(1) match {
                        case ' ' => {
                            val commits = getCommitMessages(subgroups(1), subgroups(2), subgroups(4))
                            messages :::= commits map (commit => "         "+truncateCommitMsg(commit))
                            messages ::= ":::: [new commits] " + subgroups(4)
                        }
                        case '*' => {
                            if (line contains "[new tag]")
                                messages ::= "**** [new tag] "+subgroups(4)
                            else
                                messages ::= "**** [new branch] "+subgroups(4)
                        }
                        case '+' => messages ::= "++++ [forced update] "+subgroups(4)
                        case _ => messages ::= line
                    }
                }
            }
            if (messages.size != 0) {
                messages ::= "-----------!! Repository updates !!-----------"
                sayAllChannels(messages:_*)
            }
        }
    }
}
