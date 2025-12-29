-- Module AGENCES (Agency)
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Create Agency', 'Créer une nouvelle agence', 'agency:create', 'AGENCY') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Read Agency', 'Voir les détails des agences', 'agency:read', 'AGENCY') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Update Agency', 'Modifier les informations d''une agence', 'agency:update', 'AGENCY') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Delete Agency', 'Supprimer une agence', 'agency:delete', 'AGENCY') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'List Agencies', 'Lister toutes les agences de l''organisation', 'agency:list', 'AGENCY') ON CONFLICT (tag) DO NOTHING;

-- Module PERSONNEL (Staff)
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Create Staff', 'Ajouter un nouveau membre du personnel', 'staff:create', 'STAFF') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Read Staff', 'Voir le profil d''un employé', 'staff:read', 'STAFF') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Update Staff', 'Modifier les informations d''un employé', 'staff:update', 'STAFF') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Delete Staff', 'Supprimer ou désactiver un employé', 'staff:delete', 'STAFF') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'List Staff', 'Lister tout le personnel', 'staff:list', 'STAFF') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Manage Postes', 'Créer et modifier les postes et rôles', 'staff:manage_postes', 'STAFF') ON CONFLICT (tag) DO NOTHING;

-- Module VEHICULES (Vehicle)
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Create Vehicle', 'Ajouter un véhicule à la flotte', 'vehicle:create', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Read Vehicle', 'Voir la fiche technique d''un véhicule', 'vehicle:read', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Update Vehicle', 'Mettre à jour un véhicule (km, état)', 'vehicle:update', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Delete Vehicle', 'Retirer un véhicule de la flotte', 'vehicle:delete', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'List Vehicles', 'Voir l''inventaire des véhicules', 'vehicle:list', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Manage Maintenance', 'Gérer les entretiens et réparations', 'vehicle:maintenance', 'VEHICLE') ON CONFLICT (tag) DO NOTHING;

-- Module CHAUFFEURS (Driver)
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Create Driver', 'Enregistrer un nouveau chauffeur', 'driver:create', 'DRIVER') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Read Driver', 'Voir le dossier d''un chauffeur', 'driver:read', 'DRIVER') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Update Driver', 'Mettre à jour les infos chauffeur (permis, etc.)', 'driver:update', 'DRIVER') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Delete Driver', 'Supprimer un chauffeur', 'driver:delete', 'DRIVER') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'List Drivers', 'Lister les chauffeurs disponibles', 'driver:list', 'DRIVER') ON CONFLICT (tag) DO NOTHING;
INSERT INTO permissions (id, name, description, tag, module) VALUES (gen_random_uuid(), 'Assign Driver', 'Assigner un chauffeur à une course ou un véhicule', 'driver:assign', 'DRIVER') ON CONFLICT (tag) DO NOTHING;

