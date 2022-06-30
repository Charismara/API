package com.envyful.api.forge.player;

import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.api.player.EnvyPlayer;
import com.envyful.api.player.attribute.PlayerAttribute;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Forge implementation of the {@link EnvyPlayer} interface
 *
 */
public class ForgeEnvyPlayer implements EnvyPlayer<ServerPlayerEntity> {

    protected final Map<Class<?>, PlayerAttribute<?>> attributes = Maps.newHashMap();

    private ServerPlayerEntity player;

    protected ForgeEnvyPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public UUID getUuid() {
        return this.player.getUUID();
    }

    @Override
    public String getName() {
        return this.player.getName().getString();
    }

    @Override
    public ServerPlayerEntity getParent() {
        return this.player;
    }

    public void setPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public void message(Object message) {
        if (message instanceof String) {
            this.player.sendMessage(UtilChatColour.colour((String) message), Util.NIL_UUID);
        } else if (message instanceof ITextComponent) {
            this.player.sendMessage((ITextComponent) message, Util.NIL_UUID);
        } else {
            throw new RuntimeException("Unsupported message type");
        }
    }

    @Override
    public void message(Object... messages) {
        for (Object message : messages) {
            this.message(message);
        }
    }

    @Override
    public void message(List<Object> messages) {
        for (Object message : messages) {
            this.message(message);
        }
    }

    @Override
    public void executeCommands(String... commands) {
        for (String command : commands) {
            this.executeCommand(command);
        }
    }

    @Override
    public void executeCommand(String command) {
        ServerLifecycleHooks.getCurrentServer().getCommands().performCommand(this.player.createCommandSourceStack(), command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends PlayerAttribute<B>, B> A getAttribute(Class<B> plugin) {
        return (A) this.attributes.get(plugin);
    }
}
