INSERT INTO users
(id, password, is_superuser, username, first_name, last_name, email, is_staff, is_active)
VALUES
('611d7f8a-456d-4f3c-802d-4d869dcd89bf', 'not-null', FALSE, 'test_Customer', 'John', 'Smith', 'jsmith@test.com', FALSE, TRUE),
('b651939c-96e6-4fbb-88fb-299e728e21c8', 'not-null', TRUE, 'test_Admin', 'John', 'Doe', 'jdoe@test.com', FALSE, TRUE),
('b01fae53-d742-4990-ac01-edadeb4f2e8f', 'not-null', TRUE, 'test_Suspended_Admin', 'Jonatan', 'Fired', 'jfire@test.com', FALSE, FALSE),
('75c0d9b2-2c23-41a7-93a1-d1b716cdfa6c', 'not-null', FALSE, 'test_Staff', 'Alexander', 'Great', 'greata@test.com', FALSE, TRUE);