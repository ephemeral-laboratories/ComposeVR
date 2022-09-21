import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import garden.ephemeral.composevr.singleOverlayApplication

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, VR!") }

    MaterialTheme {
        Button(onClick = {
            text = "Clicked!"
        }) {
            Text(text)
        }
    }
}

fun main() = singleOverlayApplication {
    App()
}
