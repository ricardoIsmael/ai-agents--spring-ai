-- Deja un proyecto de Supabase recien creado listo para ser la base de datos de este
-- backend. Se pega entero en el SQL Editor del dashboard y se ejecuta UNA sola vez,
-- ANTES de arrancar la aplicacion por primera vez con el perfil "supabase".


-- 1. Las extensiones que el proyecto necesita ---------------------------------------------
--
-- vector guarda los textos convertidos en numeros, que es como el buscador encuentra cosas
-- parecidas. hstore y uuid-ossp las pide Spring AI al arrancar.
--
-- Van al esquema "extensions", que es donde Supabase las pone: si se instalaran en "public"
-- ese esquema dejaria de estar vacio y Flyway se confundiria al crear las tablas.

create extension if not exists vector      with schema extensions;
create extension if not exists hstore      with schema extensions;
create extension if not exists "uuid-ossp" with schema extensions;
create extension if not exists pgcrypto    with schema extensions;


-- 2. Cerrar la puerta publica de Supabase -------------------------------------------------
--
-- Supabase publica solo, sin que nadie se lo pida, todo lo que viva en el esquema "public",
-- a traves de una API REST que se abre con la clave "anon". Esa clave es publica a
-- proposito: va dentro del navegador de cualquiera que use la aplicacion.
--
-- Aqui van a vivir nombres, correos, telefonos y curriculums de candidatos reales. Este
-- backend habla con la base por su propio puerto y con su propio usuario, asi que no
-- necesita esa API para nada. Se cierra.
--
-- La linea de "alter default privileges" es la importante: cubre tambien las tablas que
-- todavia no existen, las que creen las migraciones futuras. Sin ella habria que volver a
-- correr esto cada vez que alguien anada una tabla, y se olvidaria.

revoke usage on schema public from anon, authenticated;
revoke all on all tables    in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;
revoke all on all functions in schema public from anon, authenticated;

alter default privileges for role postgres in schema public revoke all on tables    from anon, authenticated;
alter default privileges for role postgres in schema public revoke all on sequences from anon, authenticated;
alter default privileges for role postgres in schema public revoke all on functions from anon, authenticated;


-- 3. Comprobar que el esquema public quedo vacio ------------------------------------------
--
-- Tiene que devolver 0. Si devuelve otra cosa, hay algo creado ahi y Flyway se va a negar a
-- arrancar (dira "Found non-empty schema public without schema history table"). Hay que
-- mirar que es y borrarlo, o vaciar el esquema entero con:
--     drop schema public cascade; create schema public;

select count(*) as objetos_en_public
from information_schema.tables
where table_schema = 'public';
