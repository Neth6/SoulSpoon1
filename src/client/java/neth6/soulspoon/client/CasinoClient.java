package neth6.soulspoon.client;

import neth6.soulspoon.casino.CasinoNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Llamar a CasinoClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class CasinoClient {
    private CasinoClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(CasinoNet.TablaS2C.TYPE,
                (payload, ctx) -> Minecraft.getInstance().setScreen(new CasinoScreen(payload)));
    }
}
