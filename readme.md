# gomoku ai
An implementation of an AI for reduced 8x8 gomoku, played
using threat-space search.


## definitions
1.  A **threat** is an arrangement of stones that presents the
    player with a clear opportunity to win. Important threats
    have descriptive names:
    
    1.  A *four* is a line of 5 squares, of which the attacker
        has occupied 4. The attacker can win by occupying the
        fifth square.
    2.  A *straight four* is a line of 6 squares, of which the
        attacker has occupied the four center squares, with the
        outer two empty.
    3.  A *three* is a line of seven squares with the three
        middle squares occupied by the attacker, or a line of
        six squares with three of the center four squares
        occupied.
    4.  A *broken three* is a line of six squares of which the
        attacker occupies three non-consecutively within the
        center four.
2.  If a player constructs a four, he threatens to win at the
    next move. If he constructs a straight 4, the attacker is
    guaranteed to win. If he constructs a three, the attacker
    threatens to construct a straight three at the next turn;
    while this threat has a depth of 2, it must be countered
    immediately.
3.  To win the game against any opposition, a player needs to
    create a **double threat**: either a straight four, or two
    separate threats.
4.  In most cases, a **threat sequence**, i.e. a sequence of
    moves in which each consecutive move contains a threat, is
    played before a double threat occurs. A threat sequence
    leading to a winning double threat is called a **winning
    threat sequence**. Each threat in the sequence forces the
    defender to play a move countering the threat.
    
    
## notes

We only search for attacking threat sequences at first. For
every winning threat sequence, we then look at defender
sequences. Also, to reduce the search space, we allow the
defender to play all squares.

A GAIN SQUARE of a threat is the square played by the attacker. 
A COST SQUARES of a threat are the squares played by the
defender in response. The REST SQUARES of a threat are the
squares containing a threat possibility; the expected gain
squares.

A GAIN SQUARE of a threat is the square the attacker plays. The
REST SQUARES are the rest of the squares involved in the threat.
The COST SQUARES of a threat are the squares played by the
defender to counter the threat.

A threat A is dependent on a threat B if a rest square of A
is the gain square of B (that is, the square played in B is
one of the pre-existing squares in A, i.e. in terms of moves
we have B -> A).

A dependency tree of A 