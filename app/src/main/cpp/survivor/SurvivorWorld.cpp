#include "SurvivorWorld.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kPi = 3.14159265359F;
    constexpr float kShipSizeOfWidth = 0.10F;     // smaller than Alien Shooter (0.18): bigger view
    constexpr float kMaxSpeedOfWidth = 0.55F;     // ship speed: about half a screen width per second
    constexpr float kDeadZone = 0.12F;            // ignore tiny accidental stick movements
    constexpr float kTurnSpeed = 10.0F;           // radians per second the ship can turn
    constexpr float kShipHitRadiusOfSize = 0.35F; // forgiving: only the ship's body counts
    // Laser gun: auto-aims at the nearest alien within range. Per laser level 1..6:
    constexpr float kLaserDamage[SurvivorBullets::kMaxLevel] = {1.0F, 1.4F, 1.8F, 2.2F, 2.6F, 3.0F};
    constexpr float kFireIntervalSec[SurvivorBullets::kMaxLevel] = {0.33F, 0.30F, 0.27F, 0.25F, 0.22F, 0.20F};
    // EXP: level n needs kExpPerLevel x n orbs (5, 10, 15, ...).
    constexpr int kExpPerLevel = 5;
    // Red Nuke orb: sometimes dropped instead of nothing extra; picking it up
    // destroys every alien on screen. At most one lies around at a time.
    constexpr float kNukeDropChance = 0.02F;     // share of ON-SCREEN kills that drop one (0.02 = 2%)
    constexpr float kNukeFlashSec = 0.4F;        // white flash fades out over this time
    // Ultimate style. Barrage: a non-stop spiral of fast laser shots in every
    // direction. Beams: 4 spinning laser lines (kept for later use).
    constexpr SurvivorWorld::UltimateStyle kUltimateStyle = SurvivorWorld::UltimateStyle::Barrage;
    constexpr int kBarrageDirections = 8;                 // shots per volley, evenly spread around
    constexpr float kBarrageIntervalSec = 0.08F;          // 12.5 volleys per second = 100 shots per second
    constexpr float kBarrageSpinPerSec = 2.0F;            // each volley turns a bit: the shots form spiral arms
    constexpr float kBarrageSpeedScale = 1.5F;            // faster than a normal shot
    // Beams style:
    constexpr float kBeamSpinPerSec = 2.0F * kPi / 3.0F;  // one full turn every 3 s
    constexpr float kBeamStartOfSize = 0.35F;            // beams start just outside the ship's body
    constexpr float kBeamLengthOfHeight = 0.32F;         // reaches about 2/3 of the way to the screen edge
    constexpr float kBeamHitIntervalSec = 0.1F;          // damage on a fixed clock: 10 pulses per second
    constexpr float kBeamDamage = 1.0F;                  // per pulse, to every alien a beam touches
    // UFO explosion: 22 frames at ~18 fps, same sheet as Alien Shooter.
    constexpr float kBlastSec = 1.2F;
    constexpr float kBlastSizeOfWidth = 0.09F;    // = alien size, so it lines up with the UFO

    // Shortest signed difference between two angles, in [-pi, pi].
    float angleDelta(float from, float to) {
        float d = std::fmod(to - from + kPi, 2.0F * kPi);
        if (d < 0.0F) {
            d += 2.0F * kPi;
        }
        return d - kPi;
    }
}

SurvivorWorld::SurvivorWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxBlasts,
                             std::size_t maxOrbs)
        : m_enemies(maxEnemies), m_bullets(maxBullets),
          m_blasts(maxBlasts, kBlastSec, kBlastSizeOfWidth),
          m_orbs(maxOrbs),
          m_rng(std::random_device{}()) {
    m_beamHits.reserve(maxEnemies);   // a beam can't touch more aliens than exist
    m_nukeKills.reserve(maxEnemies * 2U);
}

void SurvivorWorld::setScreen(float width, float height) {
    if (width == m_width && height == m_height) {
        return;
    }
    m_width = width;
    m_height = height;
    m_size = width * kShipSizeOfWidth;
    m_maxSpeed = width * kMaxSpeedOfWidth;
    reset();   // every size depends on the screen, so start a fresh round
}

void SurvivorWorld::reset() {
    m_x = m_y = m_prevX = m_prevY = 0.0F;
    m_angle = m_prevAngle = 0.0F;
    m_joyX = m_joyY = 0.0F;
    m_enemies.reset(m_width, m_height);
    m_bullets.reset(m_width, m_height);
    m_blasts.reset(m_width);
    m_orbs.reset(m_width);
    m_fireTimer = 0.0F;
    m_time = 0.0F;
    m_kills = 0;
    m_gameOver = false;
    m_level = 1;
    m_exp = 0;
    m_pendingUpgrades = 0;
    m_ultimate = false;
    m_beamAngle = 0.0F;
    m_prevBeamAngle = 0.0F;
    m_beamTimer = 0.0F;
    m_barrageAngle = 0.0F;
    m_barrageTimer = 0.0F;
    m_nukeFlash = 0.0F;
}

void SurvivorWorld::beginTick() {
    m_prevX = m_x;
    m_prevY = m_y;
    m_prevAngle = m_angle;
    m_prevBeamAngle = m_beamAngle;
    m_enemies.beginTick();
    m_bullets.beginTick();
    m_orbs.beginTick();
}

void SurvivorWorld::setJoystick(float x, float y) {
    // The stick is a circle: never let the input be longer than 1, so moving
    // diagonally isn't faster than moving straight.
    const float len = std::sqrt(x * x + y * y);
    if (len > 1.0F) {
        x /= len;
        y /= len;
    }
    m_joyX = x;
    m_joyY = y;
}

void SurvivorWorld::update(float dt) {
    if (!hasScreen()) {
        return;
    }
    m_blasts.update(dt);            // explosions finish playing even after game over
    m_nukeFlash = std::max(0.0F, m_nukeFlash - dt / kNukeFlashSec);
    if (m_gameOver || isChoosingUpgrade()) {
        return;                     // the level-up card freezes the round until it's answered
    }

    m_time += dt;
    movePlayer(dt);                                         // 1. ship
    m_enemies.update(dt, m_x, m_y, m_time, m_rng);          // 2. aliens spawn + chase + spread out
    m_bullets.update(dt);                                   // 3. bullets fly
    if (m_ultimate && kUltimateStyle == UltimateStyle::Beams) {
        updateBeams(dt);                                    // 4. spinning beams replace the gun
    } else if (m_ultimate) {
        updateBarrage(dt);                                  // 4. laser storm in every direction
    } else {
        autoFire(dt);                                       // 4. gun picks a target and shoots
    }
    bulletsHitEnemies();                                    // 5. hits, kills, explosions, orbs
    const SurvivorOrbs::Collected got = m_orbs.update(dt, m_x, m_y);   // 6. orbs fly in
    for (int n = 0; n < got.nukes; ++n) {
        detonateNuke();                                     //    red: clear the screen
    }
    addExp(got.exp);                                        //    green: may start a level-up
    // 7. last: an alien touching the ship ends the round (so a kill this tick can save you)
    if (m_enemies.touching(m_x, m_y, m_size * kShipHitRadiusOfSize) >= 0) {
        m_gameOver = true;
    }
}

void SurvivorWorld::movePlayer(float dt) {
    const float len = std::sqrt(m_joyX * m_joyX + m_joyY * m_joyY);
    if (len < kDeadZone) {
        return;   // stick at rest: the ship stays put
    }

    // 1. Move: direction from the stick, always at full speed (how far the
    //    stick is pushed doesn't matter, as long as it's past the dead zone).
    const float dirX = m_joyX / len;
    const float dirY = m_joyY / len;
    m_x += dirX * m_maxSpeed  * dt;
    m_y += dirY * m_maxSpeed  * dt;

    // 2. Turn towards the travel direction, at most kTurnSpeed per second, so
    //    the ship swings round smoothly instead of snapping.
    const float target = std::atan2(dirX, -dirY);   // 0 = up, clockwise
    const float maxStep = kTurnSpeed * dt;
    m_angle += std::clamp(angleDelta(m_angle, target), -maxStep, maxStep);
    m_angle = angleDelta(0.0F, m_angle);            // keep it in [-pi, pi] while circling
}

void SurvivorWorld::autoFire(float dt) {
    const float interval = kFireIntervalSec[static_cast<std::size_t>(m_bullets.level() - 1)];
    m_fireTimer += dt;
    if (m_fireTimer < interval) {
        return;
    }
    // Aim at the closest alien the bullet can actually reach.
    const int target = m_enemies.nearest(m_x, m_y, m_bullets.range());
    if (target < 0) {
        m_fireTimer = interval;   // nothing in range: fire the moment something is
        return;
    }
    const auto &e = m_enemies.enemies()[static_cast<std::size_t>(target)];
    const float dx = e.x - m_x;
    const float dy = e.y - m_y;
    const float len = std::sqrt(dx * dx + dy * dy);
    if (len < 1e-3F) {
        return;
    }
    m_bullets.fire(m_x, m_y, dx / len, dy / len);
    m_fireTimer = 0.0F;
}

void SurvivorWorld::bulletsHitEnemies() {
    const float damage = kLaserDamage[static_cast<std::size_t>(m_bullets.level() - 1)];
    for (auto &b : m_bullets.bullets()) {
        if (!b.active) {
            continue;
        }
        // The laser is long: test at its glowing tip, not its middle, so the
        // hit happens when the tip (what the eye follows) reaches the alien.
        const float speed = std::sqrt(b.vx * b.vx + b.vy * b.vy);
        if (speed < 1e-3F) {
            continue;
        }
        const float tipX = b.x + b.vx / speed * m_bullets.headOffset();
        const float tipY = b.y + b.vy / speed * m_bullets.headOffset();
        const int hit = m_enemies.touching(tipX, tipY, m_bullets.hitRadius());
        if (hit < 0) {
            continue;
        }
        b.active = false;                 // a bullet is used up by its first hit
        float x = 0.0F;
        float y = 0.0F;
        if (m_enemies.damage(hit, damage, &x, &y)) {
            onEnemyKilled(x, y);
        }
    }
}

void SurvivorWorld::updateBeams(float dt) {
    m_beamAngle = angleDelta(0.0F, m_beamAngle + kBeamSpinPerSec * dt);   // keep in [-pi, pi]
    // Damage on a fixed clock, like Alien Shooter's laser: the same at any tick rate.
    m_beamTimer += dt;
    if (m_beamTimer < kBeamHitIntervalSec) {
        return;
    }
    m_beamTimer -= kBeamHitIntervalSec;

    const float halfWidth = beamThickness() * 0.5F;
    for (int k = 0; k < kBeamCount; ++k) {
        const float a = m_beamAngle + static_cast<float>(k) * (2.0F * kPi / static_cast<float>(kBeamCount));
        const float dirX = std::sin(a);    // 0 = up, clockwise (screen y points down)
        const float dirY = -std::cos(a);
        const float x0 = m_x + dirX * beamStart();
        const float y0 = m_y + dirY * beamStart();
        const float x1 = x0 + dirX * beamLength();
        const float y1 = y0 + dirY * beamLength();
        m_enemies.touchingSegment(x0, y0, x1, y1, halfWidth, m_beamHits);
        for (const int i : m_beamHits) {
            float x = 0.0F;
            float y = 0.0F;
            if (m_enemies.damage(i, kBeamDamage, &x, &y)) {
                onEnemyKilled(x, y);
            }
        }
    }
}

void SurvivorWorld::updateBarrage(float dt) {
    m_barrageAngle = angleDelta(0.0F, m_barrageAngle + kBarrageSpinPerSec * dt);
    m_barrageTimer += dt;
    // A while loop, not an if: if a tick ever takes longer than one interval,
    // the missed volleys are still fired, so the rate is the same at any tick rate.
    while (m_barrageTimer >= kBarrageIntervalSec) {
        m_barrageTimer -= kBarrageIntervalSec;
        for (int k = 0; k < kBarrageDirections; ++k) {
            const float a = m_barrageAngle +
                            static_cast<float>(k) * (2.0F * kPi / static_cast<float>(kBarrageDirections));
            m_bullets.fire(m_x, m_y, std::sin(a), -std::cos(a), kBarrageSpeedScale);   // 0 = up, clockwise
        }
    }
}

SurvivorWorld::UltimateStyle SurvivorWorld::ultimateStyle() const {
    return m_ultimate ? kUltimateStyle : UltimateStyle::None;
}

float SurvivorWorld::beamStart() const { return m_size * kBeamStartOfSize; }

float SurvivorWorld::beamLength() const { return m_height * kBeamLengthOfHeight; }

// The beams use the yellow level-6 sprite (the laser is level 6 by then):
// a bit thicker than a level-6 shot, so they read as beams.
float SurvivorWorld::beamThickness() const {
    return m_bullets.thickness() * 1.2F;
}

void SurvivorWorld::onEnemyKilled(float x, float y, bool byNuke) {
    ++m_kills;
    m_blasts.spawn(x, y, m_rng);
    m_orbs.spawn(x, y);    // every kill drops 1 EXP
    // Only a kill the player can SEE may drop it: an orb far off-screen would
    // never be found, and (one at a time) would block every later one.
    if (!byNuke && isOnScreen(x, y) && !m_orbs.hasNukeOnGround()) {
        std::uniform_real_distribution<float> roll(0.0F, 1.0F);
        if (roll(m_rng) < kNukeDropChance) {
            // Slightly to the side, so it doesn't hide under the green orb.
            m_orbs.spawn(x + m_size * 0.3F, y, OrbKind::Nuke);
        }
    }
}

bool SurvivorWorld::isOnScreen(float x, float y) const {
    return std::fabs(x - m_x) <= m_width * 0.5F && std::fabs(y - m_y) <= m_height * 0.5F;
}

void SurvivorWorld::detonateNuke() {
    // "On screen" = the visible area around the ship (the camera follows it),
    // plus one alien size so aliens half-visible at the edge count too.
    const float margin = m_enemies.size();
    const float halfW = m_width * 0.5F + margin;
    const float halfH = m_height * 0.5F + margin;
    m_enemies.destroyInRect(m_x - halfW, m_y - halfH, m_x + halfW, m_y + halfH, m_nukeKills);
    for (std::size_t i = 0; i + 1U < m_nukeKills.size(); i += 2U) {
        onEnemyKilled(m_nukeKills[i], m_nukeKills[i + 1U], true);
    }
    m_nukeFlash = 1.0F;
}

int SurvivorWorld::expNeeded() const { return kExpPerLevel * m_level; }

int SurvivorWorld::upgradesLeft() const {
    const int laserSteps = SurvivorBullets::kMaxLevel - m_bullets.level();
    const int ultimate = m_ultimate ? 0 : 1;
    return laserSteps + ultimate - m_pendingUpgrades;
}

void SurvivorWorld::addExp(int amount) {
    m_exp += amount;
    // Several level-ups at once are possible (many orbs in one tick): each one
    // queues a card, shown one after another.
    while (m_exp >= expNeeded()) {
        m_exp -= expNeeded();
        ++m_level;
        if (upgradesLeft() > 0) {
            ++m_pendingUpgrades;   // nothing left to offer: just the level number goes up
        }
    }
}

SurvivorWorld::Upgrade SurvivorWorld::offeredUpgrade() const {
    if (m_pendingUpgrades == 0) {
        return Upgrade::None;
    }
    if (m_bullets.level() < SurvivorBullets::kMaxLevel) {
        return Upgrade::Laser;
    }
    return m_ultimate ? Upgrade::None : Upgrade::Ultimate;
}

void SurvivorWorld::chooseUpgrade(Upgrade upgrade) {
    if (upgrade == Upgrade::None || upgrade != offeredUpgrade()) {
        return;   // e.g. a double tap answering a card that is already gone
    }
    if (upgrade == Upgrade::Laser) {
        m_bullets.setLevel(m_bullets.level() + 1);
    } else {
        m_ultimate = true;
        m_beamTimer = 0.0F;
        m_barrageTimer = 0.0F;
    }
    --m_pendingUpgrades;
}
