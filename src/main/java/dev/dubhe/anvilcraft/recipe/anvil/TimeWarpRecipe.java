package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeWarpRecipe implements Recipe<TimeWarpRecipe.Input> {
    public final NonNullList<Ingredient> ingredients;
    public final Block cauldron;
    public final ItemStack result;
    public final boolean produceFluid;
    public final boolean consumeFluid;
    public final boolean fromWater;
    public final boolean isSimple;
    private Input cacheInput;
    private int cacheMaxCraftTime;

    public TimeWarpRecipe(
            NonNullList<Ingredient> ingredients,
            Block cauldron,
            ItemStack result,
            boolean produceFluid,
            boolean consumeFluid,
            boolean fromWater) {
        this.ingredients = ingredients;
        this.cauldron = cauldron;
        this.result = result;
        this.produceFluid = produceFluid;
        this.consumeFluid = consumeFluid;
        this.fromWater = fromWater;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.TIME_WARP_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TIME_WARP_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean matches(Input input, Level level) {
        if (fromWater) {
            if (input.cauldronState.is(Blocks.WATER_CAULDRON)) {
                if (input.cauldronState.getValue(LayeredCauldronBlock.LEVEL) < 3) {
                    return false;
                }
            }
        } else {
            if (consumeFluid) {
                if (!input.cauldronState.is(cauldron)) {
                    return false;
                }
            }
            if (produceFluid) {
                if (!input.cauldronState.is(cauldron) && !input.cauldronState.is(Blocks.CAULDRON)) {
                    return false;
                }
                if (input.cauldronState.is(cauldron)) {
                    if (input.cauldronState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                        if (input.cauldronState.getValue(LayeredCauldronBlock.LEVEL) >= 3) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return getMaxCraftTime(input) >= 1;
    }

    @SuppressWarnings("DuplicatedCode")
    public int getMaxCraftTime(Input pInput) {
        if (cacheInput == pInput) {
            return cacheMaxCraftTime;
        }
        int times = RecipeUtil.getMaxCraftTime(pInput, ingredients);
        if (produceFluid || consumeFluid || fromWater) {
            times = times >= 1 ? 1 : 0;
        }
        cacheInput = pInput;
        cacheMaxCraftTime = times;
        return times;
    }

    public record Input(List<ItemStack> items, BlockState cauldronState) implements RecipeInput, IItemsInput {

        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<TimeWarpRecipe> {
        private static final MapCodec<TimeWarpRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 64, "time_warp")
                                .forGetter(TimeWarpRecipe::getIngredients),
                        CodecUtil.BLOCK_CODEC.fieldOf("cauldron").forGetter(TimeWarpRecipe::getCauldron),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("result", ItemStack.EMPTY)
                                .forGetter(TimeWarpRecipe::getResult),
                        Codec.BOOL.fieldOf("produce_fluid").forGetter(TimeWarpRecipe::isProduceFluid),
                        Codec.BOOL.fieldOf("consume_fluid").forGetter(TimeWarpRecipe::isConsumeFluid),
                        Codec.BOOL.fieldOf("from_water").forGetter(TimeWarpRecipe::isFromWater))
                .apply(ins, TimeWarpRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<TimeWarpRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TimeWarpRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, TimeWarpRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, recipe.getCauldron());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.result);
            buf.writeBoolean(recipe.produceFluid);
            buf.writeBoolean(recipe.consumeFluid);
            buf.writeBoolean(recipe.fromWater);
        }

        private static TimeWarpRecipe decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            Block cauldron = CodecUtil.BLOCK_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            boolean produceFluid = buf.readBoolean();
            boolean consumeFluid = buf.readBoolean();
            boolean fromWater = buf.readBoolean();
            return new TimeWarpRecipe(ingredients, cauldron, result, produceFluid, consumeFluid, fromWater);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<TimeWarpRecipe> {

        private NonNullList<Ingredient> ingredients = NonNullList.create();
        private Block cauldron = Blocks.CAULDRON;
        private ItemStack result;
        private boolean produceFluid = false;
        private boolean consumeFluid = false;
        private boolean fromWater = false;

        public Builder requires(Ingredient ingredient, int count) {
            for (int i = 0; i < count; i++) {
                this.ingredients.add(ingredient);
            }
            return this;
        }

        public Builder requires(Ingredient ingredient) {
            return requires(ingredient, 1);
        }

        public Builder requires(ItemLike pItem, int count) {
            return requires(Ingredient.of(pItem), count);
        }

        public Builder requires(ItemLike pItem) {
            return requires(pItem, 1);
        }

        public Builder requires(TagKey<Item> pTag, int count) {
            return requires(Ingredient.of(pTag), count);
        }

        public Builder requires(TagKey<Item> pTag) {
            return requires(pTag, 1);
        }

        @Override
        public TimeWarpRecipe buildRecipe() {
            if (result == null) {
                result = ItemStack.EMPTY;
            }
            return new TimeWarpRecipe(ingredients, cauldron, result, produceFluid, consumeFluid, fromWater);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 64) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-64, RecipeId: " + pId);
            }
            if (cauldron == null) {
                throw new IllegalArgumentException("Recipe cauldron must not be null, RecipeId: " + pId);
            }
            if (result == null && (!produceFluid && !fromWater)) {
                throw new IllegalArgumentException(
                        "Recipe result must not be null when need fluid is false, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "time_warp";
        }

        @Override
        public Item getResult() {
            if (result == null) {
                return cauldron.asItem();
            }
            return result.getItem();
        }
    }
}
