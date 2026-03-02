-- 1. Créer le poste Manager Système (si n'existe pas)
INSERT INTO postes (id, organization_id, name, description, created_at)
SELECT gen_random_uuid(), NULL, 'Manager Système', 'Poste par défaut avec accès complet à la gestion d''agence', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM postes WHERE name = 'Manager Système' AND organization_id IS NULL
);

-- 2. Associer TOUTES les permissions au poste Manager Système
-- On utilise une sous-requête pour récupérer l'ID du poste qu'on vient de créer (ou qui existait)
INSERT INTO postes_permissions (poste_id, permission_id)
SELECT
    (SELECT id FROM postes WHERE name = 'Manager Système' AND organization_id IS NULL LIMIT 1),
    p.id
FROM permissions p
ON CONFLICT (poste_id, permission_id) DO NOTHING;
