export default class Autopilot {
    constructor(_options) {
        this.time = _options.time
        this.car = _options.car
        this.controls = _options.controls

        this.active = false
        this.skipping = false   // skip-to-Explorer mode
        this.currentWaypointIndex = 0
        this.pauseTimer = 0
        this.pauseLabel = ''

        // ── Stuck detection ────────────────────────────────────────────
        this.stuck = {
            timer: 0,
            lastX: 0,
            lastY: 0,
            recovering: false,
            recoverTimer: 0,
            checkInterval: 2.0,
            threshold: 0.8,
            recoverDuration: 1.4
        }

        // ── Timeline waypoints ─────────────────────────────────────────
        // Route: Intro → south on tile road → curve east through
        //        crossroads → timeline scenes → management scenes → return
        this.waypoints = [
            // ── Intro ───────────────────────────────────────────────
            { x: 0, y: 0, pause: 1.0, label: 'Depart' },

            // ── South toward crossroads (follow tile road) ──────────
            { x: 0, y: -8, pause: 0, label: '' },
            { x: 0, y: -16, pause: 0, label: '' },
            { x: 0, y: -23, pause: 0, label: '' },

            // ── Crossroads – curve east between platforms ────────────
            // Platforms sit at (0,-30), (-9,-21), (-9,-39), (9,-39), (9,-21)
            // We thread between center and NE, staying on the road
            { x: 3, y: -26, pause: 0, label: '' },
            { x: 7, y: -28, pause: 0, label: '' },
            { x: 12, y: -30, pause: 0, label: '' },
            { x: 18, y: -30, pause: 0, label: '' },
            { x: 24, y: -30, pause: 0, label: '' },

            // ── Projects timeline – scene by scene ──────────────────
            { x: 30.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 1 \u00b7 Origines de l\'agriculture' },
            { x: 54.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 2 \u00b7 Irrigation et premieres cites' },
            { x: 78.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 3 \u00b7 Outils et terrasses' },
            { x: 102.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 4 \u00b7 Agriculture medievale' },
            { x: 126.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 5 \u00b7 Revolution scientifique' },
            { x: 150.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 6 \u00b7 Revolution industrielle' },
            { x: 174.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 7 \u00b7 Chimie et conservation' },
            { x: 198.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 8 \u00b7 Mecanisation moderne' },
            { x: 222.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 9 \u00b7 Agriculture numerique' },
            { x: 246.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene 10 \u00b7 L\'avenir' },
            { x: 270.0, y: -30, pause: 3.0, threshold: 1.5, label: 'Scene finale \u00b7 Agri Film' },
            { x: 284.0, y: -30, pause: 0, threshold: 4.0, label: '' },

            // ── Explorer management panels ───────────────────────────
            { x: 304, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Delivery' },
            { x: 314.5, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Inventory' },
            { x: 325, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Appointments' },
            { x: 335.5, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Animals' },
            { x: 346, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Marketplace' },
            { x: 356.5, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Forum' },
            { x: 367, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Loans' },
            { x: 377.5, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Events' },
            { x: 388, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Trainings' },
            { x: 398.5, y: -30, pause: 3.0, threshold: 1.5, preciseStop: true, label: 'Explorer \u00b7 Help Request' },

            // ── Return west ──────────────────────────────────────────
            { x: 384, y: -30, pause: 0, label: '' },
            { x: 300, y: -30, pause: 0, label: '' },
            { x: 240, y: -30, pause: 0, label: '' },
            { x: 180, y: -30, pause: 0, label: '' },
            { x: 100, y: -30, pause: 0, label: '' },
            { x: 40, y: -30, pause: 0, label: '' },
            { x: 24, y: -30, pause: 0, label: '' },

            // ── Back through crossroads ──────────────────────────────
            { x: 14, y: -30, pause: 0, label: '' },
            { x: 8, y: -28, pause: 0, label: '' },
            { x: 3, y: -25, pause: 0, label: '' },
            { x: 0, y: -20, pause: 0, label: '' },

            // ── Back to intro ────────────────────────────────────────
            { x: 0, y: -10, pause: 0, label: '' },
            { x: 0, y: 0, pause: 1.5, label: 'Fin du parcours' },
        ]

        // Index of the first Explorer waypoint (Delivery) – index 21 in the waypoints array
        this.EXPLORER_WAYPOINT_INDEX = 21
        // Index of the last Explorer waypoint (Help Request) – index 30
        this.EXPLORER_LAST_WAYPOINT_INDEX = 30

        this.skipTargetIndex = this.EXPLORER_WAYPOINT_INDEX
        this.skipTargetReached = false
        this.pendingWaypointAdvance = false

        this.createButton()
        this.createSkipButton()
        this.createSceneLabel()
        this.setupTick()
    }

    // ─────────────────────────────── UI ────────────────────────────────

    createButton() {
        // Wrapper to hold both buttons side by side
        this.buttonWrapper = document.createElement('div')
        Object.assign(this.buttonWrapper.style, {
            position: 'fixed',
            bottom: '24px',
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            zIndex: '9999',
            userSelect: 'none',
        })
        document.body.appendChild(this.buttonWrapper)

        this.button = document.createElement('button')
        this.button.textContent = '\uD83D\uDE97 Autoconduite'

        Object.assign(this.button.style, {
            padding: '9px 20px',
            background: 'rgba(10,10,10,0.75)',
            color: '#fff',
            border: '1.5px solid rgba(255,255,255,0.4)',
            borderRadius: '24px',
            fontSize: '13px',
            fontFamily: 'sans-serif',
            cursor: 'pointer',
            backdropFilter: 'blur(4px)',
            transition: 'background 0.2s, border-color 0.2s',
            userSelect: 'none',
        })

        this.button.addEventListener('pointerenter', () => {
            this.button.style.background = 'rgba(40,40,40,0.9)'
        })
        this.button.addEventListener('pointerleave', () => {
            this.button.style.background = this.active
                ? 'rgba(20,90,20,0.85)'
                : 'rgba(10,10,10,0.75)'
        })
        this.button.addEventListener('click', () => this.toggle())

        this.buttonWrapper.appendChild(this.button)
    }

    createSkipButton() {
        this.skipButton = document.createElement('button')
        this.skipButton.textContent = '\u23ED\uFE0F Skip \u2192 Explorer'

        Object.assign(this.skipButton.style, {
            padding: '9px 18px',
            background: 'rgba(10,10,10,0.75)',
            color: '#b8ff93',
            border: '1.5px solid rgba(184,255,147,0.45)',
            borderRadius: '24px',
            fontSize: '13px',
            fontFamily: 'sans-serif',
            cursor: 'pointer',
            backdropFilter: 'blur(4px)',
            transition: 'background 0.2s, border-color 0.2s, opacity 0.3s',
            userSelect: 'none',
            display: 'none',   // hidden until autoconduite is active
        })

        this.skipButton.addEventListener('pointerenter', () => {
            this.skipButton.style.background = 'rgba(40,80,20,0.9)'
        })
        this.skipButton.addEventListener('pointerleave', () => {
            this.skipButton.style.background = 'rgba(10,10,10,0.75)'
        })
        this.skipButton.addEventListener('click', () => this.skipToExplorer())

        this.buttonWrapper.appendChild(this.skipButton)
    }

    createSceneLabel() {
        this.sceneLabelEl = document.createElement('div')
        Object.assign(this.sceneLabelEl.style, {
            position: 'fixed',
            top: '28px',
            left: '50%',
            transform: 'translateX(-50%)',
            padding: '8px 22px',
            background: 'rgba(10,10,10,0.7)',
            color: '#fff',
            border: '1px solid rgba(255,255,255,0.25)',
            borderRadius: '20px',
            fontSize: '14px',
            fontFamily: 'sans-serif',
            zIndex: '9999',
            backdropFilter: 'blur(4px)',
            opacity: '0',
            transition: 'opacity 0.4s',
            pointerEvents: 'none',
            userSelect: 'none',
            whiteSpace: 'nowrap',
        })
        document.body.appendChild(this.sceneLabelEl)
    }

    showSceneLabel(_text) {
        if (!_text) {
            this.sceneLabelEl.style.opacity = '0'
            return
        }
        this.sceneLabelEl.textContent = _text
        this.sceneLabelEl.style.opacity = '1'
    }

    setButtonActive(_active) {
        this.active = _active
        this.button.textContent = _active ? '\u23F9 Arreter' : '\uD83D\uDE97 Autoconduite'
        this.button.style.background = _active
            ? 'rgba(20,90,20,0.85)'
            : 'rgba(10,10,10,0.75)'
        this.button.style.borderColor = _active
            ? 'rgba(100,220,100,0.6)'
            : 'rgba(255,255,255,0.4)'

        // Show/hide skip button based on active state
        this.skipButton.style.display = _active ? 'block' : 'none'
    }

    // ──────────────────────────── Controls ─────────────────────────────

    toggle() {
        if (this.active) { this.stop() } else { this.start() }
    }

    skipToExplorer() {
        if (!this.active) {
            this.start()
        }

        // Enter skip mode: follow the same road but ignore pauses & use boost
        this.skipping = true
        this.skipTargetReached = false
        if (this.currentWaypointIndex > this.skipTargetIndex) {
            this.currentWaypointIndex = this.skipTargetIndex
        }
        this.pauseTimer = 0
        this.pauseLabel = ''
        this.showSceneLabel('\u23ED\uFE0F En route vers Explorer\u2026')
    }

    start() {
        this.currentWaypointIndex = 0
        this.pauseTimer = 0
        this.pauseLabel = ''
        this.skipTargetReached = false
        this.pendingWaypointAdvance = false
        this.stuck.timer = 0
        this.stuck.recovering = false
        this.stuck.recoverTimer = 0
        this.stuck.lastX = this.car.position.x
        this.stuck.lastY = this.car.position.y
        this.setButtonActive(true)
    }

    stop() {
        this.skipping = false
        this.skipTargetReached = false
        this.pendingWaypointAdvance = false
        this.pauseLabel = ''
        this.setButtonActive(false)
        this.releaseControls()
        this.showSceneLabel('')
    }

    releaseControls() {
        this.controls.actions.up = false
        this.controls.actions.down = false
        this.controls.actions.left = false
        this.controls.actions.right = false
        this.controls.actions.brake = false
        this.controls.actions.boost = false
    }

    getState() {
        return {
            active: this.active,
            currentWaypointIndex: this.currentWaypointIndex,
            pauseTimer: this.pauseTimer,
            pauseLabel: this.pauseLabel
        }
    }

    applyState(_state) {
        const hasState = _state && typeof _state === 'object'
        const waypointCount = this.waypoints.length

        this.currentWaypointIndex = hasState && typeof _state.currentWaypointIndex === 'number'
            ? Math.min(Math.max(Math.round(_state.currentWaypointIndex), 0), Math.max(waypointCount - 1, 0))
            : 0
        this.pauseTimer = hasState && typeof _state.pauseTimer === 'number'
            ? Math.max(_state.pauseTimer, 0)
            : 0
        this.pauseLabel = hasState && typeof _state.pauseLabel === 'string'
            ? _state.pauseLabel
            : ''
        this.pendingWaypointAdvance = false

        this.stuck.timer = 0
        this.stuck.recovering = false
        this.stuck.recoverTimer = 0
        this.stuck.lastX = this.car.position.x
        this.stuck.lastY = this.car.position.y

        this.releaseControls()
        this.setButtonActive(hasState && _state.active === true)

        if (this.active && this.pauseTimer > 0 && this.pauseLabel) {
            this.showSceneLabel(this.pauseLabel)
            return
        }

        this.showSceneLabel('')
    }

    // ──────────────────────────── Tick logic ───────────────────────────

    setupTick() {
        this.time.on('tick', () => {
            if (!this.active) return

            // ── Manual takeover detection ─────────────────────────────
            if (this.controls.actions.manual) {
                this.stop()
                return
            }

            const dt = this.time.delta / 1000

            // ── Stuck recovery mode ───────────────────────────────────
            if (this.stuck.recovering) {
                this.stuck.recoverTimer -= dt
                if (this.stuck.recoverTimer <= 0) {
                    // End recovery – reset tracking
                    this.stuck.recovering = false
                    this.stuck.timer = 0
                    this.stuck.lastX = this.car.position.x
                    this.stuck.lastY = this.car.position.y
                }
                else {
                    // Reverse with steering to dislodge
                    this.controls.actions.up = false
                    this.controls.actions.down = true
                    this.controls.actions.brake = false
                    this.controls.actions.boost = false
                    // Alternate turn direction during recovery
                    const half = this.stuck.recoverDuration * 0.5
                    this.controls.actions.left = this.stuck.recoverTimer > half
                    this.controls.actions.right = this.stuck.recoverTimer <= half
                    return
                }
            }

            // ── Stuck detection (only when driving forward) ───────────
            if (this.pauseTimer <= 0 && this.controls.actions.up) {
                this.stuck.timer += dt
                if (this.stuck.timer >= this.stuck.checkInterval) {
                    const sdx = this.car.position.x - this.stuck.lastX
                    const sdy = this.car.position.y - this.stuck.lastY
                    const moved = Math.sqrt(sdx * sdx + sdy * sdy)

                    if (moved < this.stuck.threshold) {
                        this.stuck.recovering = true
                        this.stuck.recoverTimer = this.stuck.recoverDuration
                        return
                    }

                    this.stuck.lastX = this.car.position.x
                    this.stuck.lastY = this.car.position.y
                    this.stuck.timer = 0
                }
            }
            else {
                // Reset stuck tracking when paused or not driving
                this.stuck.timer = 0
                this.stuck.lastX = this.car.position.x
                this.stuck.lastY = this.car.position.y
            }

            this.navigate(dt)
        })
    }

    // Normalised forward vector of the car in world XY.
    // Primary: velocity. Fallback when stopped: chassis rotation.z.
    getForward() {
        const vel = this.car.movement.speed
        const vXY = Math.sqrt(vel.x * vel.x + vel.y * vel.y)

        if (vXY > 0.4) {
            return { x: vel.x / vXY, y: vel.y / vXY }
        }

        const rotZ = this.car.chassis.object.rotation.z
        return { x: Math.cos(rotZ), y: Math.sin(rotZ) }
    }

    navigate(_dt) {
        const waypoint = this.waypoints[this.currentWaypointIndex]
        if (!waypoint) {
            this.currentWaypointIndex = 0
            return
        }

        const pos = this.car.position

        // ── Is this waypoint one of the 10 Explorer panels? ───────────
        const isExplorerWaypoint = this.currentWaypointIndex >= this.EXPLORER_WAYPOINT_INDEX
            && this.currentWaypointIndex <= this.EXPLORER_LAST_WAYPOINT_INDEX

        // ── Pause at scene ────────────────────────────────────────────
        // In skip mode, ignore pauses for timeline scenes but NOT for Explorer panels
        if (this.pauseTimer > 0 && (!this.skipping || isExplorerWaypoint)) {
            this.pauseTimer -= _dt

            this.controls.actions.up = false
            this.controls.actions.down = false
            this.controls.actions.left = false
            this.controls.actions.right = false
            this.controls.actions.brake = true
            this.controls.actions.boost = false

            if (this.pauseTimer <= 0) {
                this.pauseLabel = ''
                this.showSceneLabel('')
                if (this.pendingWaypointAdvance) {
                    this.pendingWaypointAdvance = false
                    this.currentWaypointIndex++
                    if (this.currentWaypointIndex >= this.waypoints.length) {
                        this.currentWaypointIndex = 0
                    }
                }
            }
            return
        }

        // ── Direction to waypoint ─────────────────────────────────────
        const dx = waypoint.x - pos.x
        const dy = waypoint.y - pos.y
        const dist = Math.sqrt(dx * dx + dy * dy)

        const threshold = typeof waypoint.threshold === 'number'
            ? waypoint.threshold
            : (waypoint.pause > 0 ? 1.5 : 6.0)

        if (dist < threshold) {
            // Exit skip mode once Delivery (first Explorer) is reached
            if (this.skipping && this.currentWaypointIndex === this.skipTargetIndex) {
                this.skipTargetReached = true
                this.skipping = false
                this.showSceneLabel('')
            }

            // Pause at every Explorer panel and at normal waypoints when not skipping.
            // isExplorerWaypoint is re-evaluated here using the (unchanged) current index
            // so Delivery is never skipped even right after exiting skip mode.
            if (waypoint.pause > 0 && (!this.skipping || isExplorerWaypoint)) {
                this.pauseTimer = waypoint.pause
                this.pauseLabel = waypoint.label || ''
                this.showSceneLabel(this.pauseLabel)
                this.pendingWaypointAdvance = true
                return
            }

            this.currentWaypointIndex++
            if (this.currentWaypointIndex >= this.waypoints.length) {
                this.currentWaypointIndex = 0
            }
            return
        }

        // ── Heading ───────────────────────────────────────────────────
        const fwd = this.getForward()
        const toWpX = dx / dist
        const toWpY = dy / dist

        // 2-D cross: positive → waypoint is to the LEFT
        const cross = fwd.x * toWpY - fwd.y * toWpX
        // Dot: positive → waypoint is ahead
        const dot = fwd.x * toWpX + fwd.y * toWpY

        // ── Steering ──────────────────────────────────────────────────
        const STEER_DEAD = 0.08
        this.controls.actions.left = cross > STEER_DEAD
        this.controls.actions.right = cross < -STEER_DEAD

        // ── Throttle / brake ──────────────────────────────────────────
        // Same logic as scenes — Explorer uses a shorter brake window (5 units)
        // because panels are only ~10.5 units apart.
        const slowDown = isExplorerWaypoint ? 5 : 12
        const approaching = waypoint.pause > 0
            && (!this.skipping || isExplorerWaypoint)
            && dist < slowDown

        if (approaching) {
            const forwardSpeed = Math.abs(this.car.movement.localSpeed.x)
            const crawlNearTarget = dist < Math.max(threshold + 0.6, 3.6) && forwardSpeed < 0.7

            // Prevent brake-lock before threshold: crawl the last meters.
            if (crawlNearTarget) {
                this.controls.actions.up = true
                this.controls.actions.down = false
                this.controls.actions.brake = false
                this.controls.actions.boost = false
            }
            else {
                this.controls.actions.up = false
                this.controls.actions.down = false
                this.controls.actions.brake = true
                this.controls.actions.boost = false
            }
        }
        else if (dot > -0.1) {
            this.controls.actions.up = true
            this.controls.actions.down = false
            this.controls.actions.brake = false
            this.controls.actions.boost = this.skipping
                ? (Math.abs(cross) < 0.35)
                : (dist > 30 && Math.abs(cross) < 0.25)
        }
        else {
            // Facing away – gas + steer to turn around
            this.controls.actions.up = true
            this.controls.actions.down = false
            this.controls.actions.brake = false
            this.controls.actions.boost = false
        }
    }
}