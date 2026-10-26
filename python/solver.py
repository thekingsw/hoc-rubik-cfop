"""CFOP 2-look solver (reference implementation, pure Python).

Cube model: 54 facelets, faces U=0..8, R=9..17, F=18..26, D=27..35,
L=36..44, B=45..53. Each face is indexed row-major as seen from outside
that face. Colours: 0=white, 1=red, 2=green, 3=yellow, 4=orange, 5=blue.
Colour scheme (standard CFOP working orientation): U=yellow(3),
R=orange(4), F=green(2), D=white(0), L=red(1), B=blue(5).
White cross on D, yellow last layer on U.

Move permutations are derived from 3D coordinates (no memorised tables):
every facelet has an (x, y, z) position; a move rotates one layer about an
axis. This makes the model self-verifying.
"""

# ---------------------------------------------------------------- cube model

def _facelet_pn(idx):
    """Return (position, normal) of a facelet index.

    Axes: x=right, y=up, z=front. A facelet is identified by its 3D
    position AND its outward normal (3 facelets share each corner point).
    """
    if idx < 9:      # U
        return ((idx % 3 - 1, 1, idx // 3 - 1), (0, 1, 0))
    if idx < 18:     # R
        j = idx - 9
        return ((1, 1 - j // 3, 1 - j % 3), (1, 0, 0))
    if idx < 27:     # F
        j = idx - 18
        return ((j % 3 - 1, 1 - j // 3, 1), (0, 0, 1))
    if idx < 36:     # D
        j = idx - 27
        return ((j % 3 - 1, -1, 1 - j // 3), (0, -1, 0))
    if idx < 45:     # L
        j = idx - 36
        return ((-1, 1 - j // 3, j % 3 - 1), (-1, 0, 0))
    # B
    j = idx - 45
    return ((1 - j % 3, 1 - j // 3, -1), (0, 0, -1))


def _rot_x_plus(p):
    x, y, z = p
    return (x, -z, y)


def _rot_x_minus(p):
    x, y, z = p
    return (x, z, -y)


def _rot_y_plus(p):
    x, y, z = p
    return (z, y, -x)


def _rot_y_minus(p):
    x, y, z = p
    return (-z, y, x)


def _rot_z_plus(p):
    x, y, z = p
    return (-y, x, z)


def _rot_z_minus(p):
    x, y, z = p
    return (y, -x, z)


# (axis coordinate, value, rotation) for each clockwise base move.
# Clockwise is judged looking at the face from outside.
_BASE_MOVES = {
    'U': (1, 1, _rot_y_minus),   # top layer, -90 deg about +y
    'D': (1, -1, _rot_y_plus),   # bottom layer, +90 deg about +y
    'R': (0, 1, _rot_x_minus),   # right layer, -90 deg about +x
    'L': (0, -1, _rot_x_plus),   # left layer, +90 deg about +x
    'F': (2, 1, _rot_z_minus),   # front layer, -90 deg about +z
    'B': (2, -1, _rot_z_plus),   # back layer, +90 deg about +z
    'M': (0, 0, _rot_x_plus),    # middle slice, follows L
    'E': (1, 0, _rot_y_plus),    # middle slice, follows D
    'S': (2, 0, _rot_z_minus),   # middle slice, follows F
}

_PN_TO_IDX = {pn: i for i, pn in enumerate(_facelet_pn(i) for i in range(54))}


def _layer_perm(axis, value, rot):
    """Permutation p so that new_state[i] = old_state[p[i]]."""
    perm = list(range(54))
    for i in range(54):
        pos, nrm = _facelet_pn(i)
        if pos[axis] == value:
            perm[_PN_TO_IDX[(rot(pos), rot(nrm))]] = i
    return perm


def _compose(a, b):
    """Permutation for applying a then b: new[i] = old[(a then b)[i]]."""
    # applying a: s1[i] = s0[a[i]]; applying b: s2[i] = s1[b[i]] = s0[a[b[i]]]
    return [a[b[i]] for i in range(54)]


def _rotation_perm(rot):
    """Permutation for a whole-cube rotation: every facelet moves."""
    perm = list(range(54))
    for i in range(54):
        pos, nrm = _facelet_pn(i)
        perm[_PN_TO_IDX[(rot(pos), rot(nrm))]] = i
    return perm


# base clockwise permutations
_PERM = {}
for _name, (_axis, _val, _rot) in _BASE_MOVES.items():
    _PERM[_name] = _layer_perm(_axis, _val, _rot)
    _PERM[_name + '2'] = _compose(_PERM[_name], _PERM[_name])
    _PERM[_name + "'"] = _compose(_PERM[_name + '2'], _PERM[_name])

# Whole-cube rotations (x follows R, y follows U, z follows F).
_ROTATIONS = {
    'x': _rot_x_minus, "x'": _rot_x_plus,
    'y': _rot_y_minus, "y'": _rot_y_plus,
    'z': _rot_z_minus, "z'": _rot_z_plus,
}
for _name, _rot in _ROTATIONS.items():
    _PERM[_name] = _rotation_perm(_rot)
    _PERM[_name + '2'] = _compose(_PERM[_name], _PERM[_name])

# Colour scheme (standard CFOP working orientation: yellow on top, white
# on bottom, green in front). Face -> colour, faces U,R,F,D,L,B:
#   U=yellow(3), R=orange(4), F=green(2), D=white(0), L=red(1), B=blue(5)
# Colours: 0=white, 1=red, 2=green, 3=yellow, 4=orange, 5=blue.
SCHEME = [3, 4, 2, 0, 1, 5]

# face of each facelet index, and colour of the solved cube there
_FACE_OF = [i // 9 for i in range(54)]
SOLVED = tuple(SCHEME[i // 9] for i in range(54))
MOVE_NAMES = ['U', "U'", 'U2', 'D', "D'", 'D2', 'R', "R'", 'R2',
              'L', "L'", 'L2', 'F', "F'", 'F2', 'B', "B'", 'B2',
              'M', "M'", 'M2', 'E', "E'", 'E2', 'S', "S'", 'S2']
_FACE_OF_MOVE = {m: m[0] for m in MOVE_NAMES}


def _face_of_move(m):
    return m[0]


def parse_moves(text):
    """'R U R\\' U2' -> ['R', 'U', \"R'\", 'U2']"""
    return [t for t in text.replace("’", "'").split() if t]


class Cube:
    __slots__ = ('s',)

    def __init__(self, state=None):
        self.s = tuple(state) if state is not None else SOLVED

    def copy(self):
        return Cube(self.s)

    def apply(self, move):
        p = _PERM[move]
        s = self.s
        self.s = tuple(s[p[i]] for i in range(54))
        return self

    def apply_seq(self, moves):
        for m in moves:
            self.apply(m)
        return self

    def is_solved(self):
        return self.s == SOLVED

    def __eq__(self, other):
        return isinstance(other, Cube) and self.s == other.s

    def __repr__(self):
        return 'Cube(%s)' % (self.s,)


def invert_moves(moves):
    out = []
    for m in reversed(moves):
        if m.endswith("'"):
            out.append(m[0])
        elif m.endswith('2'):
            out.append(m)
        else:
            out.append(m + "'")
    return out


def simplify_moves(moves):
    """Combine consecutive moves on the same face."""
    out = []
    for m in moves:
        if out and _face_of_move(out[-1]) == _face_of_move(m):
            prev = out.pop()
            total = _quarter_turns(prev) + _quarter_turns(m)
            total %= 4
            if total == 1:
                out.append(_face_of_move(m))
            elif total == 2:
                out.append(_face_of_move(m) + '2')
            elif total == 3:
                out.append(_face_of_move(m) + "'")
            # total == 0 -> cancel
        else:
            out.append(m)
    return out


def _quarter_turns(m):
    if m.endswith("'"):
        return 3
    if m.endswith('2'):
        return 2
    return 1

# ---------------------------------------------------------------- slots

# Edge slots as (facelet_a, facelet_b). Corner slots as triples.
EDGE_SLOTS = {
    'UR': (5, 10), 'UF': (7, 19), 'UL': (3, 37), 'UB': (1, 46),
    'DR': (32, 16), 'DF': (28, 25), 'DL': (30, 43), 'DB': (34, 52),
    'FR': (23, 12), 'FL': (21, 41), 'BR': (48, 14), 'BL': (50, 39),
}
CORNER_SLOTS = {
    'URF': (8, 9, 20), 'UFL': (6, 18, 38), 'ULB': (0, 36, 47),
    'UBR': (2, 45, 11),
    'DFR': (29, 26, 15), 'DLF': (27, 44, 24),
    'DBL': (33, 53, 42), 'DRB': (35, 17, 51),
}

# inverse permutations (sticker at f moves to _INV[m][f])
_INV = {}
for _m, _p in _PERM.items():
    _inv = [0] * 54
    for _i, _src in enumerate(_p):
        _inv[_src] = _i
    _INV[_m] = _inv

# ---------------------------------------------------------------- cross (BFS on white stickers)

WHITE_EDGES = [('WG', 2), ('WO', 4), ('WB', 5), ('WR', 1)]  # (name, other colour)
CROSS_GOAL = (28, 32, 34, 30)  # white sticker facelets: DF, DR, DB, DL


def _find_white_stickers(cube):
    """Return (wG, wR, wB, wO): facelet index of each white edge's white sticker."""
    s = cube.s
    out = []
    for _name, other in WHITE_EDGES:
        found = None
        for slot in EDGE_SLOTS.values():
            a, b = slot
            if {s[a], s[b]} == {0, other}:
                found = a if s[a] == 0 else b
                break
        if found is None:
            raise ValueError('white edge %s not found' % _name)
        out.append(found)
    return tuple(out)


def cross_solution(cube):
    """BFS for the white cross on D. Returns a list of moves.

    Face turns only: M/E/S would move the centre stickers, which the
    rest of the solver assumes stay on their faces.
    """
    from collections import deque
    face_moves = ['U', "U'", 'U2', 'D', "D'", 'D2', 'R', "R'", 'R2',
                  'L', "L'", 'L2', 'F', "F'", 'F2', 'B', "B'", 'B2']
    start = _find_white_stickers(cube)
    if start == CROSS_GOAL:
        return []
    # BFS over (white sticker positions); a move sends sticker f -> _INV[m][f]
    prev = {start: None}
    q = deque([start])
    while q:
        st = q.popleft()
        for m in face_moves:
            inv = _INV[m]
            nxt = tuple(inv[f] for f in st)
            if nxt not in prev:
                prev[nxt] = (st, m)
                if nxt == CROSS_GOAL:
                    moves = []
                    cur = nxt
                    while prev[cur] is not None:
                        cur, mv = prev[cur][0], prev[cur][1]
                        moves.append(mv)
                    moves.reverse()
                    return moves
                q.append(nxt)
    raise RuntimeError('cross BFS failed')


def cross_solved(cube):
    s = cube.s
    return (s[28] == 0 and s[25] == 2 and   # DF: white down, green front
            s[32] == 0 and s[16] == 4 and   # DR: white down, orange right
            s[34] == 0 and s[52] == 5 and   # DB: white down, blue back
            s[30] == 0 and s[43] == 1)      # DL: white down, red left


# ---------------------------------------------------------------- F2L (IDA* per pair)

# Facelets with y=1 (the top layer): U face + top rows of the side faces.
_TOP_LAYER_FACELETS = frozenset(list(range(9)) + [9, 10, 11, 18, 19, 20,
                                                 36, 37, 38, 45, 46, 47])

F2L_PAIRS = [    # (slot, corner colours (D,F/B,R/L), corner target facelets (D,F/B,R/L),
    #  edge colours, edge target facelets, label)
    ('FR', (0, 2, 4), (29, 26, 15), (2, 4), (23, 12), 'xanh lá - cam'),
    ('FL', (0, 2, 1), (27, 24, 44), (2, 1), (21, 41), 'xanh lá - đỏ'),
    ('BR', (0, 5, 4), (35, 51, 17), (5, 4), (48, 14), 'xanh dương - cam'),
    ('BL', (0, 5, 1), (33, 53, 42), (5, 1), (50, 39), 'xanh dương - đỏ'),
]

F2L_MOVES = ['U', "U'", 'U2', 'R', "R'", 'R2', 'L', "L'", 'L2',
             'F', "F'", 'F2', 'B', "B'", 'B2']


def _dist_table(target, moves):
    """BFS distances: min moves for a sticker at facelet f to reach target."""
    from collections import deque
    dist = {target: 0}
    q = deque([target])
    while q:
        f = q.popleft()
        for m in moves:
            nxt = _INV[m][f]
            if nxt not in dist:
                dist[nxt] = dist[f] + 1
                q.append(nxt)
    return dist


def _piece_primary_facelet(cube, colours):
    """Facelet index holding the first colour of a piece with given colours."""
    s = cube.s
    want = set(colours)
    for slot in list(EDGE_SLOTS.values()) + list(CORNER_SLOTS.values()):
        cols = {s[i] for i in slot}
        if cols == want:
            for i in slot:
                if s[i] == colours[0]:
                    return i
    raise ValueError('piece %s not found' % (colours,))


# ---------------------------------------------------------------- pattern databases (PDB) for F2L
# A pair's state is fully described by the facelets holding its two primary
# stickers (corner: yellow; edge: F/B colour). BFS over that pair of
# facelets, from the solved pair, gives the EXACT minimum moves to solve
# the pair ignoring everything else -- a much stronger heuristic than
# single-piece distances. Same idea for the cross (4 white stickers).

_PDB_PAIR_CACHE = {}
_PDB_CROSS = None


def _pdb_pair(corner_target, edge_target):
    """BFS over (corner primary facelet, edge primary facelet)."""
    key = (corner_target, edge_target)
    if key in _PDB_PAIR_CACHE:
        return _PDB_PAIR_CACHE[key]
    from collections import deque
    dist = {key: 0}
    q = deque([key])
    while q:
        fc, fe = q.popleft()
        d = dist[(fc, fe)]
        for m in F2L_MOVES:
            inv = _INV[m]
            nxt = (inv[fc], inv[fe])
            if nxt not in dist:
                dist[nxt] = d + 1
                q.append(nxt)
    _PDB_PAIR_CACHE[key] = dist
    return dist


def _pdb_cross():
    """BFS over the 4 white-sticker facelets, from the solved cross."""
    global _PDB_CROSS
    if _PDB_CROSS is not None:
        return _PDB_CROSS
    from collections import deque
    dist = {CROSS_GOAL: 0}
    q = deque([CROSS_GOAL])
    while q:
        st = q.popleft()
        d = dist[st]
        for m in F2L_MOVES:
            inv = _INV[m]
            nxt = tuple(inv[f] for f in st)
            if nxt not in dist:
                dist[nxt] = d + 1
                q.append(nxt)
    _PDB_CROSS = dist
    return dist


class _F2LSearch:
    def __init__(self, cube, pair_idx, solved_pairs):
        self.start = cube.copy()
        self.pair_idx = pair_idx
        self.pair = F2L_PAIRS[pair_idx]
        self.solved_pairs = solved_pairs  # list of pair indices already done
        _slot, cc, ct, ec, et, _label = self.pair
        # tracked primary facelets, in order:
        # [pair corner, pair edge, white G, white R, white B, white O,
        #  solved pair 0 corner, solved pair 0 edge, ...]
        self.piece_colours = [cc, ec]
        for _wname, other in WHITE_EDGES:
            self.piece_colours.append((0, other))
        for pi in solved_pairs:
            _s2, cc2, _ct2, ec2, _et2, _l2 = F2L_PAIRS[pi]
            self.piece_colours += [cc2, ec2]
        self.pos = [_piece_primary_facelet(self.start, colours)
                    for colours in self.piece_colours]
        # subproblems: (pdb dict, indices into self.pos forming the key)
        self.subs = [(_pdb_pair(ct[0], et[0]), (0, 1)),
                     (_pdb_cross(), (2, 3, 4, 5))]
        for k, pi in enumerate(solved_pairs):
            _s2, _cc2, ct2, _ec2, et2, _l2 = F2L_PAIRS[pi]
            base = 6 + 2 * k
            self.subs.append((_pdb_pair(ct2[0], et2[0]), (base, base + 1)))

    def heuristic(self):
        h = 0
        for dist, idxs in self.subs:
            key = tuple(self.pos[i] for i in idxs)
            d = dist.get(key, 99)
            if d > h:
                h = d
        return h

    def goal(self, cube):
        if not cross_solved(cube):
            return False
        for pi in self.solved_pairs + [self.pair_idx]:
            if not _pair_solved(cube, pi):
                return False
        return True

    def search(self, max_depth=14):
        """A* over the tracked-piece positions (pos), with a closed set.

        The goal (pair + cross + solved pairs) is fully determined by pos,
        and moves act on pos deterministically, so the search never needs
        the full cube. Each pos-state is expanded once, unlike IDA* which
        re-explores the tree at every bound -- this is what makes the last
        pair tractable.
        """
        import heapq
        start = tuple(self.pos)

        def h_of(pos):
            h = 0
            for dist, idxs in self.subs:
                key = tuple(pos[i] for i in idxs)
                d = dist.get(key, 99)
                if d > h:
                    h = d
            return h

        h0 = h_of(start)
        if h0 == 0:
            if self.goal(self.start):
                return []
            raise RuntimeError('F2L search failed for pair %s' % (self.pair[0],))
        openq = [(h0, -h0, 0, start)]
        best_g = {start: 0}
        parent = {start: (None, None)}
        while openq:
            _f, negh, g, state = heapq.heappop(openq)
            if best_g.get(state, 10 ** 9) < g:
                continue
            if -negh == 0:
                path = []
                st = state
                while parent[st][0] is not None:
                    par, mv = parent[st]
                    path.append(mv)
                    st = par
                path.reverse()
                moves = simplify_moves(path)
                # sanity check on the real cube
                check = self.start.copy()
                check.apply_seq(moves)
                if not self.goal(check):
                    raise RuntimeError('F2L A* solution failed goal check')
                return moves
            for m in F2L_MOVES:
                inv = _INV[m]
                ns = tuple(inv[p] for p in state)
                ng = g + 1
                if best_g.get(ns, 10 ** 9) <= ng:
                    continue
                nh = h_of(ns)
                if ng + nh > max_depth:
                    continue
                best_g[ns] = ng
                parent[ns] = (state, m)
                heapq.heappush(openq, (ng + nh, -nh, ng, ns))
        raise RuntimeError('F2L search failed for pair %s' % (self.pair[0],))

    def _dfs(self, cube, g, bound, path, prev_face):
        h = self.heuristic()
        f = g + h
        if f > bound:
            return False, f
        if h == 0 and self.goal(cube):
            return True, None
        min_next = None
        for m in F2L_MOVES:
            if prev_face is not None and _face_of_move(m) == prev_face:
                continue
            inv = _INV[m]
            old_pos = self.pos
            self.pos = [inv[p] for p in old_pos]
            cube.apply(m)
            path.append(m)
            found, nxt = self._dfs(cube, g + 1, bound, path, _face_of_move(m))
            if found:
                return True, None
            path.pop()
            cube.apply(invert_moves([m])[0])
            self.pos = old_pos
            if nxt is not None and (min_next is None or nxt < min_next):
                min_next = nxt
        return False, min_next


def _pair_solved(cube, pair_idx):
    _slot, cc, ct, ec, et, _label = F2L_PAIRS[pair_idx]
    s = cube.s
    return (s[ct[0]] == cc[0] and s[ct[1]] == cc[1] and s[ct[2]] == cc[2]
            and s[et[0]] == ec[0] and s[et[1]] == ec[1])


def f2l_solved(cube):
    return cross_solved(cube) and all(_pair_solved(cube, i) for i in range(4))


# ---------------------------------------------------------------- last layer (2-look, try-and-check)

LINE_ALG = parse_moves("F R U R' U' F'")
L_ALG = parse_moves("F U R U' R' F'")
CORNER_ALGS = {
    # NB: the textbook 'T' (R U R' U' R' F R F') and 'L'
    # (F R' F' R U R U' R') flip two U edges in this model, so they are
    # left out; the Sune fallback below still reaches every case.
    'Sune': parse_moves("R U R' U R U2 R'"),
    'Anti-Sune': parse_moves("R U2 R' U' R U' R'"),
    'H': parse_moves("R U R' U R U' R' U R U2 R'"),
    'Pi': parse_moves("R U2 R2 U' R2 U' R2 U2 R"),
    'U': parse_moves("R2 D R' U2 R D' R' U2 R'"),
}
PLL_CORNER_ALGS = {
    'Aa': parse_moves("x R' U R' D2 R U' R' D2 R2 x'"),
    'Ab': parse_moves("x R2 D2 R U R' D2 R U' R x'"),
    'Y': parse_moves("F R U' R' U' R U R' F' R U R' U' R' F R F'"),
}
PLL_EDGE_ALGS = {
    'Ua': parse_moves("R U' R U R U R U' R' U' R2"),
    'Ub': parse_moves("R2 U R U R' U' R' U' R' U R'"),
    'H': parse_moves("M2 U M2 U2 M2 U M2"),
    'Z': parse_moves("M2 U M2 U M' U2 M2 U2 M' U2"),
}


def _u_edges_oriented(cube):
    # Last layer is the U face, whose colour is yellow (3).
    return sum(1 for i in (1, 3, 5, 7) if cube.s[i] == 3)


def _u_corners_oriented(cube):
    return sum(1 for i in (0, 2, 6, 8) if cube.s[i] == 3)


def _try_algs(cube, algs, check):
    """Try each alg with 0..3 U turns before AND after it.

    Both matter: the pre-turn lines the algorithm up with the pieces,
    and the post-turn (AUF) rotates the layer back into place afterwards.
    Also tries a bare U turn first: sometimes the pieces only need the
    layer rotated into place and no algorithm is needed at all.
    Returns (name, moves) or None.
    """
    for r in range(4):
        test = cube.copy()
        pre = ['U'] * r
        test.apply_seq(pre)
        if check(test):
            return 'U', simplify_moves(pre)
    for name, alg in algs.items():
        for r in range(4):
            for q in range(4):
                test = cube.copy()
                mv = ['U'] * r + alg + ['U'] * q
                test.apply_seq(mv)
                if check(test):
                    return name, simplify_moves(mv)
    return None


def solve_eo(cube):
    """Orient the 4 U edges. Returns (moves, note)."""
    moves = []
    for _ in range(3):
        n = _u_edges_oriented(cube)
        if n == 4:
            return moves, ''
        if n == 0:
            cube.apply_seq(LINE_ALG)
            moves += LINE_ALG
            continue
        got = _try_algs(cube, {'Line': LINE_ALG, 'L': L_ALG},
                        lambda c: _u_edges_oriented(c) == 4)
        if got is None:
            raise RuntimeError('EO failed')
        _name, mv = got
        cube.apply_seq(mv)
        moves += mv
    if _u_edges_oriented(cube) != 4:
        raise RuntimeError('EO failed')
    return moves, ''


def solve_co(cube):
    """Orient the 4 U corners. Returns moves."""
    moves = []
    for _ in range(3):
        if _u_corners_oriented(cube) == 4:
            return moves
        got = _try_algs(cube, CORNER_ALGS,
                        lambda c: _u_corners_oriented(c) == 4)
        if got is not None:
            _name, mv = got
            cube.apply_seq(mv)
            moves += mv
            continue
        # no corner oriented: do a Sune once, then retry
        cube.apply_seq(CORNER_ALGS['Sune'])
        moves += CORNER_ALGS['Sune']
    if _u_corners_oriented(cube) != 4:
        raise RuntimeError('CO failed')
    return moves


def _u_corners_correct(cube):
    # Solved U face (yellow): URF yellow/orange/green, UFL yellow/green/red,
    # ULB yellow/red/blue, UBR yellow/blue/orange.
    s = cube.s
    return (s[8] == 3 and s[9] == 4 and s[20] == 2 and
            s[6] == 3 and s[18] == 2 and s[38] == 1 and
            s[0] == 3 and s[36] == 1 and s[47] == 5 and
            s[2] == 3 and s[45] == 5 and s[11] == 4)


def _u_edges_correct(cube):
    s = cube.s
    return (s[5] == 3 and s[10] == 4 and s[7] == 3 and s[19] == 2 and
            s[3] == 3 and s[37] == 1 and s[1] == 3 and s[46] == 5)


def solve_pll_corners(cube):
    if _u_corners_correct(cube):
        return []
    got = _try_algs(cube, PLL_CORNER_ALGS, _u_corners_correct)
    if got is None:
        raise RuntimeError('PLL corners failed')
    _name, mv = got
    cube.apply_seq(mv)
    return mv


def solve_pll_edges(cube):
    if _u_edges_correct(cube):
        return []
    got = _try_algs(cube, PLL_EDGE_ALGS,
                    lambda c: _u_edges_correct(c) and _u_corners_correct(c))
    if got is None:
        raise RuntimeError('PLL edges failed')
    _name, mv = got
    cube.apply_seq(mv)
    return mv


# ---------------------------------------------------------------- full solve

STAGE_NAMES = {
    'cross': 'Chữ thập trắng',
    'f2l': 'Cặp F2L',
    'oll_eo': 'OLL: định hướng cạnh',
    'oll_co': 'OLL: định hướng góc',
    'pll_c': 'PLL: hoán vị góc',
    'pll_e': 'PLL: hoán vị cạnh',
}


def solve(cube):
    """Solve with CFOP 2-look. Returns a list of (stage_key, title, moves).

    The input cube is NOT modified.
    """
    c = cube.copy()
    stages = []

    mv = cross_solution(c)
    c.apply_seq(mv)
    if mv:
        stages.append(('cross', STAGE_NAMES['cross'], mv))
    if not cross_solved(c):
        raise RuntimeError('cross did not solve')

    # Greedy pair order: at each step solve the pair whose pieces are
    # most in the top layer (easiest to insert while few pairs are locked).
    solved_pairs = []
    remaining = [0, 1, 2, 3]
    while remaining:
        best = None
        best_key = None
        for i in remaining:
            _slot, cc, _ct, ec, _et, _label = F2L_PAIRS[i]
            fc = _piece_primary_facelet(c, cc)
            fe = _piece_primary_facelet(c, ec)
            top_count = sum(1 for f in (fc, fe) if f in _TOP_LAYER_FACELETS)
            search = _F2LSearch(c, i, solved_pairs)
            h = search.heuristic()
            key = (-top_count, h)
            if best_key is None or key < best_key:
                best_key = key
                best = (i, search)
        i, search = best
        mv = search.search()
        c.apply_seq(mv)
        solved_pairs.append(i)
        remaining.remove(i)
        label = F2L_PAIRS[i][5]
        stages.append(('f2l', 'Cặp F2L %d/4: %s' % (len(solved_pairs), label), mv))
    if not f2l_solved(c):
        raise RuntimeError('F2L did not solve')

    mv, _ = solve_eo(c)
    if mv:
        stages.append(('oll_eo', STAGE_NAMES['oll_eo'], mv))
    mv = solve_co(c)
    if mv:
        stages.append(('oll_co', STAGE_NAMES['oll_co'], mv))
    mv = solve_pll_corners(c)
    if mv:
        stages.append(('pll_c', STAGE_NAMES['pll_c'], mv))
    mv = solve_pll_edges(c)
    if mv:
        stages.append(('pll_e', STAGE_NAMES['pll_e'], mv))

    if not c.is_solved():
        raise RuntimeError('cube not solved at the end: %s' % (c.s,))
    return stages
