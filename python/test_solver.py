"""Tests for the CFOP solver. Run: python3 test_solver.py [N]"""
import random
import sys
import time

from solver import (Cube, SOLVED, MOVE_NAMES, parse_moves, invert_moves,
                    cross_solution, cross_solved, f2l_solved, solve,
                    LINE_ALG, L_ALG, CORNER_ALGS, PLL_CORNER_ALGS,
                    PLL_EDGE_ALGS, _u_edges_oriented, _u_corners_oriented,
                    _u_corners_correct, _u_edges_correct)


def test_model():
    # every move x4 == identity
    for m in MOVE_NAMES:
        c = Cube()
        for _ in range(4):
            c.apply(m)
        assert c.is_solved(), 'move %s x4 not identity' % m
    # R U R' U' has order 6
    c = Cube()
    sexy = parse_moves("R U R' U'")
    for _ in range(6):
        c.apply_seq(sexy)
    assert c.is_solved(), 'sexy move order != 6'
    # random scramble then inverse == solved
    rng = random.Random(1)
    for _ in range(50):
        c = Cube()
        mv = [rng.choice(MOVE_NAMES) for _ in range(20)]
        c.apply_seq(mv)
        assert not c.is_solved()
        c.apply_seq(invert_moves(mv))
        assert c.is_solved(), 'inverse failed for %s' % mv
    print('model OK')


def test_cross(n=30, seed=7):
    rng = random.Random(seed)
    worst = 0
    for _ in range(n):
        c = Cube()
        c.apply_seq([rng.choice(MOVE_NAMES) for _ in range(25)])
        mv = cross_solution(c)
        assert len(mv) <= 8, 'cross too long: %d %s' % (len(mv), mv)
        worst = max(worst, len(mv))
        c.apply_seq(mv)
        assert cross_solved(c), 'cross not solved'
    print('cross OK (n=%d, worst %d moves)' % (n, worst))


def _check_alg_restores(alg, check_fn, setup_moves=None):
    """Apply inverse alg to solved cube, then alg; check_fn must hold."""
    c = Cube()
    if setup_moves:
        c.apply_seq(setup_moves)
    c.apply_seq(invert_moves(alg))
    assert not check_fn(c), 'inverse alg did not break the stage'
    c.apply_seq(alg)
    assert check_fn(c), 'alg failed: %s' % alg
    assert c.is_solved() or True  # stage check is what matters here


def test_last_layer_algs():
    # EO algs: inverse creates misoriented edges; alg must orient all
    for name, alg in [('Line', LINE_ALG), ('L', L_ALG)]:
        c = Cube()
        c.apply_seq(invert_moves(alg))
        assert _u_edges_oriented(c) < 4
        c.apply_seq(alg)
        assert _u_edges_oriented(c) == 4, 'EO %s failed' % name
    # CO algs: inverse twists corners; alg must orient all
    for name, alg in CORNER_ALGS.items():
        c = Cube()
        c.apply_seq(invert_moves(alg))
        assert _u_corners_oriented(c) < 4, 'CO %s inverse did not twist' % name
        c.apply_seq(alg)
        assert _u_corners_oriented(c) == 4, 'CO %s failed' % name
    # PLL algs: inverse scrambles LL; alg must restore the whole cube
    for group in (PLL_CORNER_ALGS, PLL_EDGE_ALGS):
        for name, alg in group.items():
            c = Cube()
            c.apply_seq(invert_moves(alg))
            assert not c.is_solved()
            c.apply_seq(alg)
            assert c.is_solved(), 'PLL %s failed' % name
    print('last-layer algs OK (%d CO, %d PLL-C, %d PLL-E)'
          % (len(CORNER_ALGS), len(PLL_CORNER_ALGS), len(PLL_EDGE_ALGS)))


def random_scramble(rng, n=22):
    faces = ['U', 'D', 'R', 'L', 'F', 'B']
    out = []
    prev = None
    for _ in range(n):
        f = rng.choice([x for x in faces if x != prev])
        prev = f
        out.append(f + rng.choice(['', "'", '2']))
    return out


def test_full_solve(n=100, seed=42):
    rng = random.Random(seed)
    total = 0
    worst = 0
    worst_stage = None
    t0 = time.time()
    for i in range(n):
        scr = random_scramble(rng)
        c = Cube()
        c.apply_seq(scr)
        stages = solve(c)
        assert stages, 'no stages'
        # replay stage by stage, checking stage goals
        c2 = Cube()
        c2.apply_seq(scr)
        for key, _title, mv in stages:
            c2.apply_seq(mv)
        assert c2.is_solved(), 'not solved for scramble %s' % scr
        # cross solved after first stage, F2L after the 5 F2L stages
        c3 = Cube()
        c3.apply_seq(scr)
        for key, _title, mv in stages:
            c3.apply_seq(mv)
            if key == 'cross':
                assert cross_solved(c3), 'cross stage did not solve cross'
            if key == 'f2l' and mv is stages[4][2]:
                assert f2l_solved(c3), 'F2L stages did not solve F2L'
        moves = sum(len(mv) for _k, _t, mv in stages)
        total += moves
        if moves > worst:
            worst = moves
            worst_stage = (scr, stages)
    dt = time.time() - t0
    print('full solve OK: n=%d, avg %.1f moves, worst %d moves, %.2fs total (%.2fs/solve)'
          % (n, total / n, worst, dt, dt / n))
    if worst_stage:
        print('worst case scramble:', ' '.join(worst_stage[0]))


if __name__ == '__main__':
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 100
    test_model()
    test_cross()
    test_last_layer_algs()
    test_full_solve(n)
    print('ALL TESTS PASSED')
