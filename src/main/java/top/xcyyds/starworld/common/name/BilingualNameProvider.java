package top.xcyyds.starworld.common.name;

import net.minecraft.util.RandomSource;

import java.util.concurrent.atomic.AtomicReference;

public interface BilingualNameProvider {
    AtomicReference<BilingualNameProvider> INSTANCE = new AtomicReference<>(new DefaultBilingualNameProvider());

    static BilingualNameProvider get() {
        return INSTANCE.get();
    }

    static void set(BilingualNameProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        INSTANCE.set(provider);
    }

    BilingualName generateNationName(RandomSource random);

    BilingualName generateNpcName(RandomSource random);

    final class DefaultBilingualNameProvider implements BilingualNameProvider {
        @Override
        public BilingualName generateNationName(RandomSource random) {
            return NameGenerator.generateNationName(random);
        }

        @Override
        public BilingualName generateNpcName(RandomSource random) {
            return NameGenerator.generateNpcName(random);
        }
    }
}
