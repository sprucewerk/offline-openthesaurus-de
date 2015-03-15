
```
CREATE VIRTUAL TABLE termFts3 USING FTS3(id bigint(20),
version bigint(20),
is_acronym bit(1),
is_short_form bit(1),
language_id bigint(20),
level_id bigint(20),
normilized_word varchar(255),
original_id integer,
synset_id bigint(20),
user_comment varchar(255),
word varchar(255),
word_grammar_id bigint(20)
);
```


Gekürzte Version:

```
CREATE VIRTUAL TABLE term USING FTS3(
_id bigint(20),
level_id bigint(20),
normalized_word varchar(255),
synset_id bigint(20),
word varchar(255)
);
```

Beispielaufruf:


SELECT DISTINCT word,