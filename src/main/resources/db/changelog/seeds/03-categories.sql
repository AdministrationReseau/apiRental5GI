INSERT INTO vehicle_categories (id, organization_id, name, description) VALUES
(gen_random_uuid(), NULL, 'Berline (Sedan)', 'Véhicule de tourisme standard, confortable pour 4-5 passagers.'),
(gen_random_uuid(), NULL, 'SUV (4x4)', 'Véhicule tout-terrain spacieux, idéal pour les routes difficiles.'),
(gen_random_uuid(), NULL, 'Luxe (Luxury)', 'Véhicule haut de gamme pour événements VIP et confort maximal.'),
(gen_random_uuid(), NULL, 'Bus / Minibus', 'Transport de groupe (9 places et plus).'),
(gen_random_uuid(), NULL, 'Camion (Truck)', 'Véhicule utilitaire lourd pour le transport de marchandises.'),
(gen_random_uuid(), NULL, 'Pick-up', 'Véhicule utilitaire léger avec benne ouverte.'),
(gen_random_uuid(), NULL, 'Électrique / Hybride', 'Véhicule écologique à faible consommation.')
ON CONFLICT (name) DO NOTHING;
