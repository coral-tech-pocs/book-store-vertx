-------- Publisher Table -------------------------
CREATE TABLE IF NOT Exists public.publisher
(
  id integer NOT NULL,
  name character varying(100) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT publisher_name_unique UNIQUE (name)
);
----------------------------------------------------
-------- Author Table ------------------------------
CREATE TABLE IF NOT Exists public.author
(
  id integer NOT NULL,
  name character varying(100) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT author_name_unique UNIQUE (name)
);
------------------------------------------------------
-------- Book Table ----------------------------------
CREATE TABLE IF NOT Exists public.book
(
  id serial NOT NULL,
  isbn character varying(13) NOT NULL,
  title character varying(150) NOT NULL,
  description character varying(500),
  price integer NOT NULL,
  publisher_id integer,
  PRIMARY KEY (id),
  CONSTRAINT book_isbn_title_const UNIQUE (isbn, title),
  CONSTRAINT book_publisher FOREIGN KEY (publisher_id)
    REFERENCES public.publisher (id) MATCH SIMPLE
    ON UPDATE CASCADE
    ON DELETE SET NULL
    NOT VALID
);
---------------------------------------------------------
-------- Book_Author Table ------------------------------
CREATE TABLE IF NOT Exists public.book_author
(
  id integer NOT NULL,
  book_id integer NOT NULL,
  author_id integer NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT "book_FK" FOREIGN KEY (book_id)
    REFERENCES public.book (id) MATCH SIMPLE
    ON UPDATE CASCADE
    ON DELETE SET NULL
    NOT VALID,
  CONSTRAINT "author_FK" FOREIGN KEY (author_id)
    REFERENCES public.author (id) MATCH SIMPLE
    ON UPDATE CASCADE
    ON DELETE SET NULL
    NOT VALID
);
