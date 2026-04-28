package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class MixinItemStack {
	@ModifyReturnValue(method = "use", at = @At("RETURN"))
	protected InteractionResult openAtlasWithItem(InteractionResult original, Level world, Player user, InteractionHand hand) {
		return world.isClientSide() && original == InteractionResult.PASS && AntiqueAtlas.isHandheldAtlas(user.getItemInHand(hand)) && AntiqueAtlas.openAtlasScreen() != null ? InteractionResult.SUCCESS : original;
	}
}
