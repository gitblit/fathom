CREATE TABLE accounts(id IDENTITY, username VARCHAR(255) UNIQUE, password VARCHAR(255), name VARCHAR(255), email VARCHAR(255));
INSERT INTO accounts(username, password, name, email) VALUES('gjetson', 'astro', 'George Jetson', 'george@spacelyspacesprockets.com');
INSERT INTO accounts(username, password, name, email) VALUES('jjetson', 'george', 'Jane Jetson', 'jane@spacelyspacesprockets.com');

CREATE TABLE account_roles(username VARCHAR(255), role VARCHAR(255));
INSERT INTO account_roles VALUES('gjetson', 'buttonpusher');
INSERT INTO account_roles VALUES('jjetson', 'admin');

CREATE TABLE account_permissions(username VARCHAR(255), permission VARCHAR(255));
INSERT INTO account_permissions VALUES('gjetson', 'powers:sleeping');
CREATE TABLE defined_roles(role VARCHAR(255), definition VARCHAR(255));
INSERT INTO defined_roles VALUES('admin', 'secure:*;powers:*');
