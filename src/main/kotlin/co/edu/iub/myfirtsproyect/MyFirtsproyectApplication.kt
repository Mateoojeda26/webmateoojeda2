package co.edu.iub.myfirtsproyect

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class TaskoraPetApplication

fun main(args: Array<String>) {
    runApplication<TaskoraPetApplication>(*args)

}
