#ifndef MY_CHATROOM_SURVIVORGRID_H
#define MY_CHATROOM_SURVIVORGRID_H


#include <algorithm>
#include <cmath>
#include <vector>

// Uniform grid ("spatial hash") for fast "what is near this point?" questions.
//
// The area around the player is cut into square cells. Every tick the grid is
// rebuilt: each object is filed under the cell it stands in. A query then only
// looks at the cells its circle overlaps, instead of at every object.
//
//   without grid: every bullet x every enemy, every enemy x every enemy
//   with grid:    each object x the few objects in the cells around it
//
// The grid is a fixed-size square that follows the player (the world is
// endless, but only the area around the player matters). Objects outside it
// are filed under the nearest border cell, so nothing is ever lost: border
// cells just get fuller.
//
// Storage is two flat arrays (no per-cell lists, no allocation per tick):
//   m_entries   : object indices, grouped by cell
//   m_cellStart : m_cellStart[c] .. m_cellStart[c + 1] is cell c's slice of m_entries
class SurvivorGrid {
public:
    // cellSize: use at least the largest distance at which two objects can
    // interact. coverRadius: how far around the centre the grid reaches.
    void configure(float cellSize, float coverRadius, std::size_t maxItems) {
        m_cellSize = std::max(1.0F, cellSize);
        m_side = std::max(1, static_cast<int>(std::ceil(2.0F * coverRadius / m_cellSize)));
        const std::size_t cells = at(m_side) * at(m_side);
        m_cellStart.assign(cells + 1U, 0);
        m_cursor.assign(cells, 0);
        m_entries.assign(maxItems, 0);
        m_cellOf.assign(maxItems, -1);
        m_count = 0;
    }

    // Files `count` objects into cells, with the grid centred on (centerX, centerY).
    // getPos(i, x, y) writes object i's position and returns false to skip it
    // (e.g. an inactive pool slot). This is a counting sort: two passes, O(count).
    template <class GetPos>
    void build(int count, float centerX, float centerY, GetPos &&getPos) {
        const float halfWidth = 0.5F * static_cast<float>(m_side) * m_cellSize;
        m_originX = centerX - halfWidth;
        m_originY = centerY - halfWidth;
        std::fill(m_cellStart.begin(), m_cellStart.end(), 0);

        // Pass 1: which cell is each object in, and how many per cell?
        for (int i = 0; i < count; ++i) {
            float x = 0.0F;
            float y = 0.0F;
            if (!getPos(i, x, y)) {
                m_cellOf[at(i)] = -1;
                continue;
            }
            const int cell = row(y) * m_side + col(x);
            m_cellOf[at(i)] = cell;
            ++m_cellStart[at(cell + 1)];
        }
        // Running total: cell c's slice starts where the cells before it end.
        for (std::size_t c = 1; c < m_cellStart.size(); ++c) {
            m_cellStart[c] += m_cellStart[c - 1];
        }

        // Pass 2: drop each object into the next free place of its cell's slice.
        std::copy(m_cellStart.begin(), m_cellStart.end() - 1, m_cursor.begin());
        for (int i = 0; i < count; ++i) {
            const int cell = m_cellOf[at(i)];
            if (cell >= 0) {
                const int slot = m_cursor[at(cell)]++;
                m_entries[at(slot)] = i;
            }
        }
        m_count = count;
    }

    // Calls visit(index) for every object filed in a cell that the circle
    // (x, y, radius) overlaps. These are CANDIDATES: the caller still does the
    // exact distance test. visit returns false to stop early.
    // Works for any radius: a bullet touches ~4 cells, a rocket blast more.
    template <class Visit>
    void forEachCandidate(float x, float y, float radius, Visit &&visit) const {
        if (m_count == 0) {
            return;
        }
        const int c0 = col(x - radius);
        const int c1 = col(x + radius);
        const int r0 = row(y - radius);
        const int r1 = row(y + radius);
        for (int r = r0; r <= r1; ++r) {
            for (int c = c0; c <= c1; ++c) {
                const int cell = r * m_side + c;
                for (int k = m_cellStart[at(cell)]; k < m_cellStart[at(cell + 1)]; ++k) {
                    if (!visit(m_entries[at(k)])) {
                        return;
                    }
                }
            }
        }
    }

    int cellsPerSide() const { return m_side; }
    float cellSize() const { return m_cellSize; }

private:
    // MISRA: no hidden int -> size_t conversion when indexing a vector.
    // Every index in this class is >= 0 (cells are clamped, -1 is checked first).
    static std::size_t at(int i) { return static_cast<std::size_t>(i); }

    // World coordinate -> cell column/row, clamped to the grid's border.
    int col(float x) const {
        return std::clamp(static_cast<int>(std::floor((x - m_originX) / m_cellSize)), 0, m_side - 1);
    }
    int row(float y) const {
        return std::clamp(static_cast<int>(std::floor((y - m_originY) / m_cellSize)), 0, m_side - 1);
    }

    float m_cellSize = 1.0F;
    float m_originX = 0.0F, m_originY = 0.0F;   // world position of the grid's top-left corner
    int m_side = 1;                             // cells per side
    int m_count = 0;
    std::vector<int> m_cellStart, m_cursor, m_entries, m_cellOf;
};

#endif //MY_CHATROOM_SURVIVORGRID_H
