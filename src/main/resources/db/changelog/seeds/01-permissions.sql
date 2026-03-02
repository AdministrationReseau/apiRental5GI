INSERT INTO permissions (id, name, description, tag, module) VALUES
-- VEHICLE
(gen_random_uuid(), 'Créer un véhicule', 'Permet d''ajouter un véhicule à la flotte', 'vehicle:create', 'VEHICLE'),
(gen_random_uuid(), 'Modifier un véhicule', 'Permet de modifier les infos d''un véhicule', 'vehicle:update', 'VEHICLE'),
(gen_random_uuid(), 'Supprimer un véhicule', 'Permet de retirer un véhicule (archivage)', 'vehicle:delete', 'VEHICLE'),
(gen_random_uuid(), 'Lister les véhicules', 'Voir la liste des véhicules de l''agence', 'vehicle:list', 'VEHICLE'),
(gen_random_uuid(), 'Voir détails véhicule', 'Voir la fiche complète d''un véhicule', 'vehicle:view', 'VEHICLE'),
(gen_random_uuid(), 'Gérer statut véhicule', 'Mettre en maintenance ou disponible', 'vehicle:manage_status', 'VEHICLE'),

-- DRIVER
(gen_random_uuid(), 'Créer un chauffeur', 'Ajouter un nouveau chauffeur', 'driver:create', 'DRIVER'),
(gen_random_uuid(), 'Modifier un chauffeur', 'Modifier infos chauffeur', 'driver:update', 'DRIVER'),
(gen_random_uuid(), 'Supprimer un chauffeur', 'Retirer un chauffeur', 'driver:delete', 'DRIVER'),
(gen_random_uuid(), 'Lister les chauffeurs', 'Voir la liste des chauffeurs', 'driver:list', 'DRIVER'),
(gen_random_uuid(), 'Voir détails chauffeur', 'Voir le profil complet', 'driver:view', 'DRIVER'),
(gen_random_uuid(), 'Gérer planning chauffeur', 'Définir les indisponibilités', 'driver:schedule', 'DRIVER'),

-- RENTAL
(gen_random_uuid(), 'Créer une location (Walk-in)', 'Créer une location au comptoir', 'rental:create', 'RENTAL'),
(gen_random_uuid(), 'Lister les locations', 'Voir l''historique et les locations en cours', 'rental:list', 'RENTAL'),
(gen_random_uuid(), 'Valider départ', 'Confirmer le départ du véhicule (Check-out)', 'rental:start', 'RENTAL'),
(gen_random_uuid(), 'Valider retour', 'Confirmer le retour et l''état (Check-in)', 'rental:validate_return', 'RENTAL'),
(gen_random_uuid(), 'Annuler location', 'Annuler une réservation client', 'rental:cancel', 'RENTAL'),
(gen_random_uuid(), 'Voir détails location', 'Voir le contrat et les paiements', 'rental:view', 'RENTAL'),

-- STAFF
(gen_random_uuid(), 'Créer un employé', 'Ajouter un membre au staff', 'staff:create', 'STAFF'),
(gen_random_uuid(), 'Modifier un employé', 'Modifier les infos d''un collègue', 'staff:update', 'STAFF'),
(gen_random_uuid(), 'Supprimer un employé', 'Désactiver un compte staff', 'staff:delete', 'STAFF'),
(gen_random_uuid(), 'Lister le personnel', 'Voir l''annuaire de l''agence', 'staff:list', 'STAFF'),

-- AGENCY
(gen_random_uuid(), 'Modifier infos agence', 'Changer horaires, adresse, contact', 'agency:update', 'AGENCY'),
(gen_random_uuid(), 'Voir infos agence', 'Voir le profil public de l''agence', 'agency:view', 'AGENCY'),

-- STATS & FINANCE
(gen_random_uuid(), 'Voir Dashboard', 'Accès aux graphiques de performance', 'stats:dashboard', 'STATS'),
(gen_random_uuid(), 'Voir Rapports Financiers', 'Accès aux revenus détaillés', 'stats:financial', 'STATS'),
(gen_random_uuid(), 'Voir Transactions', 'Lister l''historique des paiements', 'finance:transactions', 'FINANCE'),

-- CATEGORY
(gen_random_uuid(), 'Gérer catégories', 'Créer/Modifier catégories locales', 'category:manage', 'VEHICLE')

ON CONFLICT (tag) DO NOTHING;
