package co.edu.iub.myfirtsproyect.support

import co.edu.iub.myfirtsproyect.model.NotificationChannel
import co.edu.iub.myfirtsproyect.model.NotificationChannelType
import co.edu.iub.myfirtsproyect.model.User
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

// Kotlin rejects the null values Mockito matchers return for non-null parameters,
// so these helpers register the matcher and hand back a real placeholder instance.
fun captureString(captor: ArgumentCaptor<String>): String {
    captor.capture()
    return ""
}

fun anyUser(): User {
    Mockito.any(User::class.java)
    return User()
}

fun anyChannel(): NotificationChannel {
    Mockito.any(NotificationChannel::class.java)
    return NotificationChannel(type = NotificationChannelType.EMAIL, destination = "", owner = User())
}
