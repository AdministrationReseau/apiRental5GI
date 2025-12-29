-- Nettoyage si besoin (attention en prod)
-- DELETE FROM vehicle_categories WHERE organization_id IS NULL;

-- Insertion des catégories standards du marché
INSERT INTO vehicle_categories (id, organization_id, name, description) 
VALUES 
(gen_random_uuid(), NULL, 'Citadine', 'Petites voitures compactes idéales pour la ville (ex: Toyota Yaris, Renault Clio)'),
(gen_random_uuid(), NULL, 'Berline', 'Voitures confortables pour longs trajets (ex: Toyota Camry, Mercedes Classe C)'),
(gen_random_uuid(), NULL, 'SUV / 4x4', 'Véhicules tout-terrain ou urbains surélevés (ex: Toyota Prado, Hyundai Santa Fe)'),
(gen_random_uuid(), NULL, 'Luxe', 'Véhicules haut de gamme et prestige (ex: Range Rover, Mercedes Classe S)'),
(gen_random_uuid(), NULL, 'Utilitaire / Van', 'Véhicules de transport de marchandises ou grand volume (ex: Toyota Hiace)'),
(gen_random_uuid(), NULL, 'Pick-up', 'Véhicules utilitaires avec benne, robustes (ex: Toyota Hilux, Mitsubishi L200)'),
(gen_random_uuid(), NULL, 'Économique', 'Modèles d''entrée de gamme à faible consommation carburant')
ON CONFLICT (name) DO NOTHING; -- Nécessite un index unique sur le nom pour fonctionner