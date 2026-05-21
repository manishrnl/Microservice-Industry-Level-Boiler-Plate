INSERT INTO roles(name)
VALUES ('SUPER_ADMIN'),
       ('ADMIN'),
       ('EDITOR'),
       ('CREATOR'),
       ('VIEWER'),
       ('USER') ON CONFLICT DO NOTHING;

INSERT INTO permissions(name, resource, action)
VALUES ('system.config.manage', 'system', 'manage'),
       ('users.manage', 'users', 'manage'),
       ('audit.export', 'audit', 'export'),
       ('content.manage.all', 'content', 'manage_all'),
       ('content.manage.own', 'content', 'manage_own'),
       ('content.read.published', 'content', 'read_published'),
       ('profile.read.own', 'profile', 'read_own'),
       ('profile.update.own', 'profile', 'update_own') ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN' ON CONFLICT DO NOTHING;
