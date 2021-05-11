-- ################################################

create table attachment
(
    id   integer primary key,
    name text,
    data bytea
);

insert into attachment (id, name, data)
values (1, 'a', E'\\xDEADBEEF'),
       (2, 'b', E'\\xCAFEBABE'),
       (3, 'c', E'\\xFACEFEED');

insert into attachment (id, name, data)
values (1, 'a', E'\\xDEADBEEF');


-- ################################################

create table chat
(
    id      integer primary key,
    message jsonb
);

insert into chat (id, message)
values (1, '[
    {
        "title": "Sleeping Beauties",
        "genres": [
            "Fiction",
            "Thriller",
            "Horror"
        ],
        "published": false
    },
    {
        "title": "The Dictator''s Handbook",
        "genres": [
            "Law",
            "Politics"
        ],
        "authors": [
            "Bruce Bueno de Mesquita",
            "Alastair Smith"
        ],
        "published": true
    }
]');

insert into chat (id, message)
values (2, '[
    {
        "title": "Sleeping Beauties",
        "genres": [
            "Fiction",
            "Thriller",
            "Horror"
        ],
        "published": false
    },
    {
        "title": "The Dictator''s Handbook",
        "genres": [
            "Law",
            "Politics"
        ],
        "authors": [
            "Bruce Bueno de Mesquita",
            "Alastair Smith"
        ],
        "published": true
    }
]');
