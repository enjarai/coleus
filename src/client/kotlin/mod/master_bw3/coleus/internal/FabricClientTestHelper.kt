/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mod.master_bw3.coleus.internal

import mod.master_bw3.coleus.mixin.client.CyclingButtonWidgetAccessor
import mod.master_bw3.coleus.mixin.client.ScreenAccessor
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.screen.world.LevelLoadingScreen
import net.minecraft.client.gui.widget.*
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.option.Perspective
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.Text
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier


// Provides thread safe utils for interacting with a running game.
public object FabricClientTestHelper {
    public fun waitForLoadingComplete() {
        FabricClientTestHelper.waitFor(
            "Loading to complete",
            Predicate { client: MinecraftClient? -> client!!.getOverlay() == null },
            Duration.ofMinutes(5)
        )
    }

    public fun waitForScreen(screenClass: Class<out Screen?>) {
        waitFor(
            String.format("Screen %s", screenClass.getName()),
            Predicate { client: MinecraftClient? -> client!!.currentScreen != null && client.currentScreen?.javaClass == screenClass })
    }

    public fun openGameMenu() {
        FabricClientTestHelper.setScreen(Function { client: MinecraftClient? -> GameMenuScreen(true) })
        waitForScreen(GameMenuScreen::class.java)
    }

    public fun openInventory() {
        FabricClientTestHelper.setScreen(Function { client: MinecraftClient? ->
            InventoryScreen(
                Objects.requireNonNull<ClientPlayerEntity?>(
                    client!!.player
                )
            )
        })

        val creative =
            FabricClientTestHelper.submitAndWait<Boolean?>(java.util.function.Function { client: net.minecraft.client.MinecraftClient ->
                java.util.Objects.requireNonNull<ClientPlayerEntity?>(
                    client.player
                ).isCreative()
            })!!
        waitForScreen(if (creative) CreativeInventoryScreen::class.java else InventoryScreen::class.java)
    }

    public fun closeScreen() {
        FabricClientTestHelper.setScreen(Function { client: MinecraftClient? -> null })
    }

    private fun setScreen(screenSupplier: Function<MinecraftClient?, Screen?>) {
        FabricClientTestHelper.submit<Any?>(Function { client: MinecraftClient? ->
            client!!.setScreen(screenSupplier.apply(client))
            null
        })
    }

    public fun takeScreenshot(name: String?) {
        // Allow time for any screens to open
        FabricClientTestHelper.waitFor(Duration.ofSeconds(1))

        submitAndWait<Any?>(Function { client: MinecraftClient? ->
            ScreenshotRecorder.saveScreenshot(
                FabricLoader.getInstance().getGameDir().toFile(),
                name + ".png",
                client!!.getFramebuffer(),
                Consumer { message: Text? -> })
            null
        })
    }

    public fun clickScreenButton(translationKey: String?) {
        val buttonText = Text.translatable(translationKey).getString()

        waitFor("Click button" + buttonText, Predicate { client: MinecraftClient? ->
            val screen = client!!.currentScreen
            if (screen == null) {
                return@Predicate false
            }

            val screenAccessor = screen as ScreenAccessor

            for (drawable in screenAccessor.getDrawables()) {
                if (drawable is PressableWidget && FabricClientTestHelper.pressMatchingButton(drawable, buttonText)) {
                    return@Predicate true
                }

                if (drawable is Widget) {
                    drawable.forEachChild(Consumer { clickableWidget: ClickableWidget? ->
                        FabricClientTestHelper.pressMatchingButton(
                            clickableWidget,
                            buttonText
                        )
                    })
                }
            }
            false
        })
    }

    private fun pressMatchingButton(widget: ClickableWidget?, text: String): Boolean {
        if (widget is ButtonWidget) {
            if (text == widget.getMessage().getString()) {
                widget.onPress()
                return true
            }
        }

        if (widget is CyclingButtonWidget<*>) {
            val accessor = widget as CyclingButtonWidgetAccessor

            if (text == accessor.getOptionText().getString()) {
                widget.onPress()
                return true
            }
        }

        return false
    }

    public fun waitForWorldTicks(ticks: Long) {
        // Wait for the world to be loaded and get the start ticks
        FabricClientTestHelper.waitFor(
            "World load",
            Predicate { client: MinecraftClient? -> client!!.world != null && client.currentScreen !is LevelLoadingScreen },
            Duration.ofMinutes(30)
        )
        val startTicks =
            FabricClientTestHelper.submitAndWait<Long>(java.util.function.Function { client: net.minecraft.client.MinecraftClient -> client.world!!.time })!!
        FabricClientTestHelper.waitFor("World load", Predicate { client: MinecraftClient? ->
            Objects.requireNonNull<ClientWorld>(
                client!!.world
            ).getTime() > startTicks + ticks
        }, Duration.ofMinutes(10))
    }

    //	public static void enableDebugHud() {
    //		submitAndWait(client -> {
    //			client.options.debugEnabled = true;
    //			return null;
    //		});
    //	}
    public fun setPerspective(perspective: Perspective?) {
        submitAndWait<Any?>(Function { client: MinecraftClient? ->
            client!!.options.setPerspective(perspective)
            null
        })
    }

    private fun waitFor(
        what: String?,
        predicate: Predicate<MinecraftClient?>,
        timeout: Duration = Duration.ofSeconds(10)
    ) {
        val end = LocalDateTime.now().plus(timeout)

        while (true) {
            val result =
                FabricClientTestHelper.submitAndWait<Boolean?>(java.util.function.Function { t: net.minecraft.client.MinecraftClient? ->
                    predicate.test(
                        t
                    )
                })!!

            if (result) {
                break
            }

            if (LocalDateTime.now().isAfter(end)) {
                throw RuntimeException("Timed out waiting for " + what)
            }

            FabricClientTestHelper.waitFor(Duration.ofSeconds(1))
        }
    }

    private fun waitFor(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    private fun <T> submit(function: Function<MinecraftClient, T>): CompletableFuture<T> {
        return MinecraftClient.getInstance().submit<T>(Supplier { function.apply(MinecraftClient.getInstance()) })
    }

    public fun <T> submitAndWait(function: Function<MinecraftClient, T>): T {
        return FabricClientTestHelper.submit<T>(function).join()
    }
}
