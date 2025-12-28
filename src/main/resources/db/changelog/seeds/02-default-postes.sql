-- 1. Création du Poste Manager (ID fixe pour référence système)
INSERT INTO postes (id, organization_id, name, description, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'Manager d''Agence', 'Gestionnaire opérationnel : gère le staff, les véhicules et les chauffeurs de son agence.', NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Attribution des permissions limitées
-- On exclut explicitement la création et la suppression d'agences
INSERT INTO postes_permissions (poste_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions
WHERE (tag LIKE 'agency:read' OR tag LIKE 'agency:update')
   OR tag LIKE 'staff:%'
   OR tag LIKE 'vehicle:%'
   OR tag LIKE 'driver:%'
ON CONFLICT DO NOTHING;
