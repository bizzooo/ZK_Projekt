DROP TABLE IF EXISTS public.heroes;
CREATE TABLE public.heroes (
    id UUID PRIMARY KEY,
    dota_hero_id INTEGER NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    localized_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(255),
    icon_url VARCHAR(255)
);
INSERT INTO public.heroes (id, dota_hero_id, name, localized_name, image_url, icon_url)
SELECT RANDOM_UUID(), X,
       CONCAT('npc_dota_hero_', CAST(X AS VARCHAR)),
       CONCAT('Hero ', LPAD(CAST(X AS VARCHAR), 5, '0')),
       'https://example.invalid/hero.png',
       'https://example.invalid/icon.png'
FROM SYSTEM_RANGE(1, 20000);
CREATE INDEX idx_heroes_lookup_order ON public.heroes(localized_name, dota_hero_id);
