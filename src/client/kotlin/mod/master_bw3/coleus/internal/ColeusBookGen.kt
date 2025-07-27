package mod.master_bw3.coleus.internal

import io.wispforest.lavender.book.BookLoader
import mod.master_bw3.coleus.internal.FabricClientTestHelper.clickScreenButton
import mod.master_bw3.coleus.internal.FabricClientTestHelper.openGameMenu
import mod.master_bw3.coleus.internal.FabricClientTestHelper.submitAndWait
import mod.master_bw3.coleus.internal.FabricClientTestHelper.waitForLoadingComplete
import mod.master_bw3.coleus.internal.FabricClientTestHelper.waitForScreen
import mod.master_bw3.coleus.internal.FabricClientTestHelper.waitForWorldTicks
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screen.ConfirmScreen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.util.Identifier
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

internal object ColeusBookGen {
    internal fun run() {
        val thread = Thread(Runnable {
            try {
                runBookGen()
            } catch (t: Throwable) {
                t.printStackTrace()
                exitProcess(1)
            }
        })
        thread.setName("Coleus Book Generation")
        thread.start()
    }

    internal fun runBookGen() {
        waitForLoadingComplete()

        val onboardAccessibility: Boolean = submitAndWait({ client -> client.options.onboardAccessibility })

        if (onboardAccessibility) {
            waitForScreen(AccessibilityOnboardingScreen::class.java)
            clickScreenButton("gui.continue")
        }

        run {
            waitForScreen(TitleScreen::class.java)
            clickScreenButton("menu.singleplayer")
        }

        if (!isDirEmpty(FabricLoader.getInstance().gameDir.resolve("saves"))) {
            waitForScreen(SelectWorldScreen::class.java)
            clickScreenButton("selectWorld.create")
        }

        run {
            waitForScreen(CreateWorldScreen::class.java)
            clickScreenButton("selectWorld.gameMode")
            clickScreenButton("selectWorld.gameMode")
            clickScreenButton("selectWorld.create")
        }

        // API test mods use experimental features
        try {
            waitForScreen(ConfirmScreen::class.java)
            clickScreenButton("gui.yes")
        } catch (e: RuntimeException) {

        }

        submitAndWait {
            val bookId = System.getenv("COLEUS_BOOK_ID")
            val books = if (bookId != null) {
                listOf(BookLoader.get(Identifier.of(bookId))!!)
            } else {
                BookLoader.loadedBooks()
            }
            books.forEach { HtmlBookGenerator(it).generate() }
        }

        run {
            openGameMenu()
            clickScreenButton("menu.returnToMenu")
        }

        run {
            waitForScreen(TitleScreen::class.java)
            clickScreenButton("menu.quit")
        }
    }

    private fun isDirEmpty(path: Path): Boolean {
        try {
            Files.newDirectoryStream(path).use { directory ->
                return !directory.iterator().hasNext()
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
