import * as THREE from 'three'
import Materials from './Materials.js'
import Floor from './Floor.js'
import Shadows from './Shadows.js'
import Physics from './Physics.js'
import Zones from './Zones.js'
import Objects from './Objects.js'
import Car from './Car.js'
import Areas from './Areas.js'
import Tiles from './Tiles.js'
import Walls from './Walls.js'
import IntroSection from './Sections/IntroSection.js'
import ProjectsSection from './Sections/ProjectsSection.js'
import CrossroadsSection from './Sections/CrossroadsSection.js'
import InformationSection from './Sections/InformationSection.js'
import PlaygroundSection from './Sections/PlaygroundSection.js'
// import DistinctionASection from './Sections/DistinctionASection.js'
// import DistinctionBSection from './Sections/DistinctionBSection.js'
// import DistinctionCSection from './Sections/DistinctionCSection.js'
// import DistinctionDSection from './Sections/DistinctionDSection.js'
import Trees from './Trees.js'
import Controls from './Controls.js'
import Sounds from './Sounds.js'
import gsap from 'gsap'
import EasterEggs from './EasterEggs.js'
import Autopilot from './Autopilot.js'

export default class World
{
    constructor(_options)
    {
        // Options
        this.config = _options.config
        this.debug = _options.debug
        this.resources = _options.resources
        this.time = _options.time
        this.sizes = _options.sizes
        this.camera = _options.camera
        this.scene = _options.scene
        this.renderer = _options.renderer
        this.passes = _options.passes

        // Debug
        if(this.debug)
        {
            this.debugFolder = this.debug.addFolder('world')
            this.debugFolder.open()
        }

        // Set up
        this.container = new THREE.Object3D()
        this.container.matrixAutoUpdate = false
        this.started = false

        // this.setAxes()
        this.setSounds()
        this.setControls()
        this.setFloor()
        this.setAreas()
        this.setSessionState()
        this.setStartingScreen()
    }

    start()
    {
        if(this.started)
        {
            return
        }

        this.started = true

        window.setTimeout(() =>
        {
            this.camera.pan.enable()
        }, 2000)

        this.setReveal()
        this.setMaterials()
        this.setShadows()
        this.setPhysics()
        this.setZones()
        this.setObjects()
        this.setCar()
        this.areas.car = this.car
        this.setWelcomePanel()
        this.setTiles()
        this.setWalls()
        this.setTrees()
        this.setSections()
        this.setEasterEggs()
        this.setAutopilot()
        this.setProgressTracking()
    }

    setReveal()
    {
        this.reveal = {}
        this.reveal.matcapsProgress = 0
        this.reveal.floorShadowsProgress = 0
        this.reveal.previousMatcapsProgress = null
        this.reveal.previousFloorShadowsProgress = null

        // Go method
        this.reveal.go = (_options = {}) =>
        {
            const preserveCarState = _options.preserveCarState === true

            gsap.fromTo(this.reveal, { matcapsProgress: 0 }, { matcapsProgress: 1, duration: 3 })
            gsap.fromTo(this.reveal, { floorShadowsProgress: 0 }, { floorShadowsProgress: 1, duration: 3, delay: 0.5 })
            gsap.fromTo(this.shadows, { alpha: 0 }, { alpha: 0.5, duration: 3, delay: 0.5 })

            if(this.sections.intro)
            {
                gsap.fromTo(this.sections.intro.instructions.arrows.label.material, { opacity: 0 }, { opacity: 1, duration: 0.3, delay: 0.5 })
                if(this.sections.intro.otherInstructions)
                {
                    gsap.fromTo(this.sections.intro.otherInstructions.label.material, { opacity: 0 }, { opacity: 1, duration: 0.3, delay: 0.75 })
                }
            }

            // Car
            this.physics.car.chassis.body.sleep()

            if(!preserveCarState)
            {
                this.physics.car.chassis.body.position.set(0, 0, 12)
                this.physics.car.chassis.body.velocity.set(0, 0, 0)
                this.physics.car.chassis.body.angularVelocity.set(0, 0, 0)
            }

            window.setTimeout(() =>
            {
                this.physics.car.chassis.body.wakeUp()
            }, 300)

            // Sound
            gsap.fromTo(this.sounds.engine.volume, { master: 0 }, { master: 0.7, duration: 0.5, delay: 0.3, ease: 'power2.in' })
            window.setTimeout(() =>
            {
                this.sounds.play('reveal')
            }, 400)

            // Controls
            if(this.controls.touch)
            {
                window.setTimeout(() =>
                {
                    this.controls.touch.reveal()
                }, 400)
            }
        }

        // Time tick
        this.time.on('tick',() =>
        {
            // Matcap progress changed
            if(this.reveal.matcapsProgress !== this.reveal.previousMatcapsProgress)
            {
                // Update each material
                for(const _materialKey in this.materials.shades.items)
                {
                    const material = this.materials.shades.items[_materialKey]
                    material.uniforms.uRevealProgress.value = this.reveal.matcapsProgress
                }

                // Save
                this.reveal.previousMatcapsProgress = this.reveal.matcapsProgress
            }

            // Matcap progress changed
            if(this.reveal.floorShadowsProgress !== this.reveal.previousFloorShadowsProgress)
            {
                // Update each floor shadow
                for(const _mesh of this.objects.floorShadows)
                {
                    _mesh.material.uniforms.uAlpha.value = this.reveal.floorShadowsProgress
                }

                // Save
                this.reveal.previousFloorShadowsProgress = this.reveal.floorShadowsProgress
            }
        })

        // Debug
        if(this.debug)
        {
            this.debugFolder.add(this.reveal, 'matcapsProgress').step(0.0001).min(0).max(1).name('matcapsProgress')
            this.debugFolder.add(this.reveal, 'floorShadowsProgress').step(0.0001).min(0).max(1).name('floorShadowsProgress')
            this.debugFolder.add(this.reveal, 'go').name('reveal')
        }
    }

    setStartingScreen()
    {
        this.startingScreen = {}

        // Area
        this.startingScreen.area = this.areas.add({
            position: new THREE.Vector2(0, 0),
            halfExtents: new THREE.Vector2(2.35, 1.5),
            hasKey: false,
            testCar: false,
            active: false
        })

        // Loading label
        this.startingScreen.loadingLabel = {}
        this.startingScreen.loadingLabel.geometry = new THREE.PlaneGeometry(2.5, 2.5 / 4)
        this.startingScreen.loadingLabel.image = new Image()
        this.startingScreen.loadingLabel.image.src = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAQAAAABABAMAAAAHc7SNAAAAMFBMVEUAAAD///9ra2ucnJzR0dH09PQmJiaNjY24uLjp6end3d1CQkLFxcVYWFiqqqp9fX3nQ5qrAAAEVUlEQVRo3u3YT08TQRQA8JEtW6CATGnDdvljaTwYE2IBI/HGRrwSetGTsZh4MPFQYiQe229gE++WePFY9Oqh1cRzieEDYIgXLxjPJu5M33vbZQszW+fgoS+B7ewO836znRl2lg1jGMP4P2Okw0yFvaKsklr3I99Tvl3iPPelGbQhKqxB4eN6N/7gVcsvbEAz1F4RLn67zzl/v6/oLvejGBQ9LsNphio4UFjmEAsVJuOK/zkDtc6w+gyTcZ3LyP6IAzjBDA+pj6LkEgAjW4kANsMAC6vmOvqAMU5RgVOTskQACicCmCcA9AXjkT5gj1MswqlxWcoTgKJ6HuAQAD5guNoAu8QpMnBul1ONMGD2PCBbRgDAKYq6AEtmXvtdj3S6GhRyW1t1DvkAgM0ggG7mu1t3xWFHFzAqv3wYCi0mY1UCGgiQPU+1oWIY8LoXcAA3qeYfr+kClvHW14PJ5OfCAgHYNAoDAORBQIrDvHjqH5c0ANTbORzBacbAQgUC2IAKAzI9gCSHlWEMLmgBPJxMvyARpIICALDm4nkAbwIA71EZx5UOgO48JnLoOhQIAN9sOgKoBoAE5r0aB8ARcNhtFzrg0VQmwCp8CAMeAADGc44S5GMBsF1aCEU2LcAcAPDCvwFytBDehCaUgJxRAKeF8BNUUQJ43iiAUlqwFKoBrTCAHjiagwEgU0YM5IYWYD4KoIgPwIXQwUbVgCXzgLpIBJNeDciWTQNskVsq1ADX/6kYBdCTjse5owbMiX+IpgGWOCPSuWpA2vN/TAMm5QTYg5IC4FdbMA0YF5Nb5s2rAaLyhzBgektGZWDArrgqi0U1QHxf38OABDwUDgTAjGfyPlTVgJT/67FBACbqyGYaaoBctQwD2vI4DecVAPkgZRhQlxPQks2rAePGAbZsRlaa1QBYEQBUHRCAmaXD0QDYxgFWdye05R9cDQCrmQYkeBA6gGXTgNEeQF4DMG4S4MLjOUZRA5A0CcjADgmjqgGwSwSg9wK1GIBS74KTgTxv/EHoiaVQsTOS5RoCJuiZyosB8EIrHpyowFiYofO0i4wCjhCQwL0hq2sCaFNM22S4JXloLk0AuLDTBzCBAAt3xykeA7CHe/mDbgdTvQ9GswSAwdbqA0giYASHjQUJnhQKhQ6z/d8rDA4hAG2Dsk042ejubHMM2nV6AMf93pCkaRjhh0WsWuz+6aasl2FwiAImReEts1/CSaFfwFouAJxC4RW+I4oCThBQE1X2WbKkBFDkqYDtJ0SHaYKq3pJJwCECjjiFPoC1w+2P0gumurgeBjT6AhIIGKOelGIAngWlFnRnMZjMIYBb7gtIIsAuYU+8GICpEhYyZVgIZ2g9rYYAX1lfAKvjnxzjnWrHALDn9K1h2k2aoI1ewGd2AWAVAVMHcKdW4wDYje739pNufJXhkJohgLu9zy4CHCKAJYUge4ddCojGyPrp9kaHmYjUi9N7+2wYwxjGZfEXMKxGE0GkkfIAAAAASUVORK5CYII='
        this.startingScreen.loadingLabel.texture = new THREE.Texture(this.startingScreen.loadingLabel.image)
        this.startingScreen.loadingLabel.texture.magFilter = THREE.NearestFilter
        this.startingScreen.loadingLabel.texture.minFilter = THREE.LinearFilter
        this.startingScreen.loadingLabel.texture.needsUpdate = true
        this.startingScreen.loadingLabel.material = new THREE.MeshBasicMaterial({ transparent: true, depthWrite: false, color: 0xffffff, alphaMap: this.startingScreen.loadingLabel.texture })
        this.startingScreen.loadingLabel.mesh = new THREE.Mesh(this.startingScreen.loadingLabel.geometry, this.startingScreen.loadingLabel.material)
        this.startingScreen.loadingLabel.mesh.matrixAutoUpdate = false
        this.container.add(this.startingScreen.loadingLabel.mesh)

        // Start label
        this.startingScreen.startLabel = {}
        this.startingScreen.startLabel.geometry = new THREE.PlaneGeometry(2.5, 2.5 / 4)
        this.startingScreen.startLabel.image = new Image()
        this.startingScreen.startLabel.image.src = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAQAAAABABAMAAAAHc7SNAAAAMFBMVEUAAAD///+cnJxra2vR0dHd3d0mJib09PRYWFjp6em4uLhCQkKqqqqNjY19fX3FxcV3XeRgAAADsklEQVRo3u3YsU9TQRwH8KNgLSDQg9ZCAak1IdE4PKPu1NTEsSzOMDl3I3GpcXAxBhLjXFxNjJgQJ2ON0Rnj4uAAEyv8B/L7tV++5/VN+CM69Ldwfa+534d7d793VzeIQQzi/49c4v5lPF/1vvhFm++rjIpcyErrmrSCuz+cxng1iL/If8drPJD2Lc/Iy4VhaZWlFd4tLPfuMc6e/5LvRilJA2SkVSQA8c0OsI0uNtIAU9rsB8y1rAAZjyimAUa1mQDAeGwF+MA+9lIA69qs9AMKVoDP8vhf35A+NiMAc7YJKFSrX7tcI8BW9+k/O/kz6zSunjSnncMHiQYBcmdXrh3xCVbc2WO8N/YZZI0AxxwMArKivmwAwFKSPmV0UwBbCpj5E+C+yzUbQAaJVwUSA9SFjwFgHQ0jAMrBWgzAPCtHgFFbQAlpEwKC2zWUQgJGbAH+naSdu/fTxQAthPL5/ADD6OCpQwCAsb6LsbEGcBluOAYBmG2fkMIawHVWXEsDIGUGpZCAIRsAS93DPgDbhUmUQgKe2NUB90hfhK0YwEJYHkYpJGDbqBKiB86CGLAlzd6/S8CEvh8sACiBvrSXCshKblWEgNy2vkAMAHwGfjECcJHOu5qUQgDm6vXulshZAXJNL9GJAeg+LxeKPQBj1gzgdlnuCWAhbOi7LwaU9u0A2VWPpUgAC+GR5k0iwBtnB3Bj3qMaRYB17X0IOQhYcjYA7guxxyIAGfd1HNqchPfly7aACQUshAA2W1r5G1yG415YpgB3qIIkAHBH2D075QnQ10fHDsCl+CoGSKpiN8kMAVqIN00BsitnVgKyPIBMB4ADKU92AA5BKQIgszjKBGBLagpwB5xZBGS6pbcuizQAXMA6NAK86OCQ3okAI55BQPe7VoDxXzU/iwPASgS4GAASAiYxWgYAzvAa1loA2AkAFQIU2zEELCJtDDgIAG0CFLvp7LblC2kAtF6eTEJJ2CBAr88bAXKY4WkASbzXmwt5AvTvohHA4WSUBmj2Jt+IThQChrAOLQC13vPFMAOAQwuyTAeAKVQto3OBDOdESh2YxNZPbpYBQNbEAoBfod7e1i1BiwB0voSZWgwAOWgtAGPhD18E8ASIiRIAXNPwXJBtcqMbAFAIr5weIJMAcIx1aAAIqk0lAuycompyFwBMHAsAZlj/lgw0rsy2AkhbsgK4Q+70CUBjxeFXsUb0G1HJDJC9rketZRcCWCJwHM8DgJm7b7ch+XizXm25QQxiEOcXvwGCWOhbCZC0qAAAAABJRU5ErkJggg=='
        this.startingScreen.startLabel.texture = new THREE.Texture(this.startingScreen.startLabel.image)
        this.startingScreen.startLabel.texture.magFilter = THREE.NearestFilter
        this.startingScreen.startLabel.texture.minFilter = THREE.LinearFilter
        this.startingScreen.startLabel.texture.needsUpdate = true
        this.startingScreen.startLabel.material = new THREE.MeshBasicMaterial({ transparent: true, depthWrite: false, color: 0xffffff, alphaMap: this.startingScreen.startLabel.texture })
        this.startingScreen.startLabel.material.opacity = 0
        this.startingScreen.startLabel.mesh = new THREE.Mesh(this.startingScreen.startLabel.geometry, this.startingScreen.startLabel.material)
        this.startingScreen.startLabel.mesh.matrixAutoUpdate = false
        this.container.add(this.startingScreen.startLabel.mesh)

        // Progress
        this.resources.on('progress', (_progress) =>
        {
            // Update area
            this.startingScreen.area.floorBorder.material.uniforms.uAlpha.value = 1
            this.startingScreen.area.floorBorder.material.uniforms.uLoadProgress.value = _progress
        })

        // Ready
        this.resources.on('ready', () =>
        {
            window.requestAnimationFrame(() =>
            {
                if(this.resumeProgress)
                {
                    this.hideStartingScreen({ immediate: true })
                    this.beginExperience({ resumeProgress: this.resumeProgress, revealDelay: 120 })
                    return
                }

                this.startingScreen.area.activate()

                gsap.to(this.startingScreen.area.floorBorder.material.uniforms.uAlpha, { value: 0.3, duration: 0.3 })
                gsap.to(this.startingScreen.loadingLabel.material, { opacity: 0, duration: 0.3 })
                gsap.to(this.startingScreen.startLabel.material, { opacity: 1, duration: 0.3, delay: 0.3 })
            })
        })

        // On interact, reveal
        this.startingScreen.area.on('interact', () =>
        {
            this.hideStartingScreen()
            this.beginExperience({ revealDelay: 600 })
        })
    }

    setSessionState()
    {
        this.progressTrackingSet = false
        this.resumeProgress = null
        this.sessionState = {
            progressKey: 'greenroots:explorer:progress:v1',
            tutorialKey: 'greenroots:explorer:tutorial:v1',
            saveThrottle: 850,
            lastSaveAt: 0,
            progressStorage: null,
            tutorialStorage: null
        }

        try
        {
            this.sessionState.progressStorage = window.sessionStorage
        }
        catch(_error)
        {
            this.sessionState.progressStorage = null
        }

        try
        {
            this.sessionState.tutorialStorage = window.sessionStorage
        }
        catch(_error)
        {
            this.sessionState.tutorialStorage = null
        }

        if(!this.sessionState.progressStorage && this.sessionState.tutorialStorage)
        {
            this.sessionState.progressStorage = this.sessionState.tutorialStorage
        }

        this.resumeProgress = this.readSavedProgress()

        this.handlePageHide = () =>
        {
            this.saveProgress({ force: true })
        }

        this.handleVisibilityChange = () =>
        {
            if(document.hidden)
            {
                this.saveProgress({ force: true })
            }
        }

        window.addEventListener('pagehide', this.handlePageHide)
        window.addEventListener('beforeunload', this.handlePageHide)
        document.addEventListener('visibilitychange', this.handleVisibilityChange)
    }

    beginExperience(_options = {})
    {
        if(this.started)
        {
            return
        }

        const resumeProgress = _options.resumeProgress || null
        const revealDelay = typeof _options.revealDelay === 'number' ? _options.revealDelay : 600

        this.start()

        window.setTimeout(() =>
        {
            if(resumeProgress)
            {
                this.restoreProgress(resumeProgress)
                this.reveal.go({ preserveCarState: true })
                return
            }

            this.reveal.go()

            if(this.shouldShowWelcomePanel())
            {
                this.showWelcomePanel()
            }
        }, revealDelay)
    }

    hideStartingScreen(_options = {})
    {
        const immediate = _options.immediate === true

        this.startingScreen.area.deactivate()

        if(immediate)
        {
            this.startingScreen.area.floorBorder.material.uniforms.uAlpha.value = 0
            this.startingScreen.area.floorBorder.material.uniforms.uProgress.value = 0
            this.startingScreen.loadingLabel.material.opacity = 0
            this.startingScreen.startLabel.material.opacity = 0
            return
        }

        gsap.to(this.startingScreen.area.floorBorder.material.uniforms.uProgress, { value: 0, duration: 0.3, delay: 0.4 })
        gsap.to(this.startingScreen.area.floorBorder.material.uniforms.uAlpha, { value: 0, duration: 0.3, delay: 0.2 })
        gsap.to(this.startingScreen.loadingLabel.material, { opacity: 0, duration: 0.2 })
        gsap.to(this.startingScreen.startLabel.material, { opacity: 0, duration: 0.3, delay: 0.4 })
    }

    readSavedProgress()
    {
        if(!this.sessionState.progressStorage)
        {
            return null
        }

        try
        {
            const raw = this.sessionState.progressStorage.getItem(this.sessionState.progressKey)
            if(!raw)
            {
                return null
            }

            const progress = JSON.parse(raw)

            if(!progress || progress.version !== 1 || progress.started !== true || !progress.car)
            {
                this.sessionState.progressStorage.removeItem(this.sessionState.progressKey)
                return null
            }

            return progress
        }
        catch(_error)
        {
            return null
        }
    }

    saveProgress(_options = {})
    {
        if(!this.started || !this.physics || !this.car || !this.autopilot || !this.sessionState.progressStorage)
        {
            return
        }

        const now = this.time.elapsed
        if(!_options.force && now - this.sessionState.lastSaveAt < this.sessionState.saveThrottle)
        {
            return
        }

        const body = this.physics.car.chassis.body
        const progress = {
            version: 1,
            started: true,
            savedAt: Date.now(),
            car: {
                position: { x: body.position.x, y: body.position.y, z: body.position.z },
                quaternion: { x: body.quaternion.x, y: body.quaternion.y, z: body.quaternion.z, w: body.quaternion.w },
                velocity: { x: body.velocity.x, y: body.velocity.y, z: body.velocity.z },
                angularVelocity: { x: body.angularVelocity.x, y: body.angularVelocity.y, z: body.angularVelocity.z }
            },
            autopilot: this.autopilot.getState()
        }

        try
        {
            this.sessionState.progressStorage.setItem(this.sessionState.progressKey, JSON.stringify(progress))
            this.sessionState.lastSaveAt = now
        }
        catch(_error)
        {
        }
    }

    restoreProgress(_progress)
    {
        if(!_progress || !_progress.car || !this.physics || !this.car)
        {
            return
        }

        const body = this.physics.car.chassis.body
        const savedCar = _progress.car

        this.controls.actions.up = false
        this.controls.actions.down = false
        this.controls.actions.left = false
        this.controls.actions.right = false
        this.controls.actions.brake = false
        this.controls.actions.boost = false
        this.controls.actions.manual = false

        body.sleep()
        body.position.set(savedCar.position.x, savedCar.position.y, savedCar.position.z)
        body.quaternion.set(savedCar.quaternion.x, savedCar.quaternion.y, savedCar.quaternion.z, savedCar.quaternion.w)
        body.velocity.set(savedCar.velocity.x, savedCar.velocity.y, savedCar.velocity.z)
        body.angularVelocity.set(savedCar.angularVelocity.x, savedCar.angularVelocity.y, savedCar.angularVelocity.z)

        if(body.previousPosition)
        {
            body.previousPosition.set(savedCar.position.x, savedCar.position.y, savedCar.position.z)
        }

        if(body.interpolatedPosition)
        {
            body.interpolatedPosition.set(savedCar.position.x, savedCar.position.y, savedCar.position.z)
        }

        if(body.previousQuaternion)
        {
            body.previousQuaternion.set(savedCar.quaternion.x, savedCar.quaternion.y, savedCar.quaternion.z, savedCar.quaternion.w)
        }

        if(body.interpolatedQuaternion)
        {
            body.interpolatedQuaternion.set(savedCar.quaternion.x, savedCar.quaternion.y, savedCar.quaternion.z, savedCar.quaternion.w)
        }

        if(body.initPosition)
        {
            body.initPosition.set(savedCar.position.x, savedCar.position.y, savedCar.position.z)
        }

        if(body.initQuaternion)
        {
            body.initQuaternion.set(savedCar.quaternion.x, savedCar.quaternion.y, savedCar.quaternion.z, savedCar.quaternion.w)
        }

        this.car.chassis.object.position.copy(body.position).add(this.car.chassis.offset)
        this.car.chassis.object.quaternion.copy(body.quaternion)
        this.car.position.copy(this.car.chassis.object.position)

        if(this.autopilot)
        {
            this.autopilot.applyState(_progress.autopilot)
        }

        body.wakeUp()
        this.resumeProgress = null
        this.saveProgress({ force: true })
    }

    setProgressTracking()
    {
        if(this.progressTrackingSet)
        {
            return
        }

        this.progressTrackingSet = true

        this.time.on('tick', () =>
        {
            this.saveProgress()
        })
    }

    shouldShowWelcomePanel()
    {
        if(!this.sessionState.tutorialStorage)
        {
            return true
        }

        return this.sessionState.tutorialStorage.getItem(this.sessionState.tutorialKey) !== '1'
    }

    markWelcomePanelAsShown()
    {
        if(!this.sessionState.tutorialStorage)
        {
            return
        }

        try
        {
            this.sessionState.tutorialStorage.setItem(this.sessionState.tutorialKey, '1')
        }
        catch(_error)
        {
        }
    }

    setWelcomePanel()
    {
        this.welcomePanel = {}
        this.welcomePanel.state = { opacity: 0 }
        this.welcomePanel.group = new THREE.Object3D()
        this.welcomePanel.group.visible = false
        this.welcomePanel.group.matrixAutoUpdate = false
        this.container.add(this.welcomePanel.group)

        const texture = this.createWelcomePanelTexture()
        texture.needsUpdate = true

        this.welcomePanel.glowMaterial = new THREE.MeshBasicMaterial({
            color: 0x7de66a,
            transparent: true,
            opacity: 0,
            depthWrite: false
        })
        this.welcomePanel.glow = new THREE.Mesh(new THREE.PlaneGeometry(7.5, 4.25), this.welcomePanel.glowMaterial)
        this.welcomePanel.glow.position.set(0, 0, -0.02)
        this.welcomePanel.group.add(this.welcomePanel.glow)

        this.welcomePanel.material = new THREE.MeshBasicMaterial({
            map: texture,
            transparent: true,
            opacity: 0,
            depthWrite: false,
            side: THREE.DoubleSide
        })
        this.welcomePanel.mesh = new THREE.Mesh(new THREE.PlaneGeometry(6.9, 3.9), this.welcomePanel.material)
        this.welcomePanel.group.add(this.welcomePanel.mesh)

        this.time.on('tick', () =>
        {
            if(!this.welcomePanel || !this.car || !this.camera.instance)
            {
                return
            }

            const opacity = this.welcomePanel.state.opacity
            this.welcomePanel.material.opacity = opacity
            this.welcomePanel.glowMaterial.opacity = opacity * 0.16

            if(opacity <= 0.001)
            {
                this.welcomePanel.group.visible = false
                return
            }

            const hover = Math.sin(this.time.elapsed * 0.004) * 0.14
            const offset = new THREE.Vector3(5.4, 0, 4.15 + hover)
            offset.applyQuaternion(this.car.chassis.object.quaternion)

            this.welcomePanel.group.visible = true
            this.welcomePanel.group.position.copy(this.car.chassis.object.position).add(offset)
            this.welcomePanel.group.quaternion.copy(this.camera.instance.quaternion)
            this.welcomePanel.group.updateMatrix()
        })
    }

    showWelcomePanel()
    {
        if(!this.welcomePanel)
        {
            return
        }

        this.markWelcomePanelAsShown()
        this.welcomePanel.group.visible = true
        gsap.killTweensOf(this.welcomePanel.state)
        this.welcomePanel.state.opacity = 0

        gsap.to(this.welcomePanel.state, {
            opacity: 1,
            duration: 0.7,
            ease: 'power2.out'
        })

        gsap.to(this.welcomePanel.state, {
            opacity: 0,
            duration: 0.8,
            delay: 9.2,
            ease: 'power2.in',
            onComplete: () =>
            {
                this.welcomePanel.group.visible = false
            }
        })
    }

    createWelcomePanelTexture()
    {
        const canvas = document.createElement('canvas')
        canvas.width = 1024
        canvas.height = 576
        const context = canvas.getContext('2d')

        if(!context)
        {
            return new THREE.CanvasTexture(canvas)
        }

        context.clearRect(0, 0, canvas.width, canvas.height)

        context.fillStyle = 'rgba(4, 16, 8, 0.45)'
        this.drawRoundedRect(context, 48, 56, 928, 464, 44)
        context.fill()

        const gradient = context.createLinearGradient(72, 72, 920, 488)
        gradient.addColorStop(0, '#10331a')
        gradient.addColorStop(0.6, '#1d5f31')
        gradient.addColorStop(1, '#2f8c47')
        context.fillStyle = gradient
        this.drawRoundedRect(context, 36, 40, 928, 464, 40)
        context.fill()

        context.strokeStyle = 'rgba(214, 255, 205, 0.55)'
        context.lineWidth = 4
        this.drawRoundedRect(context, 48, 52, 904, 440, 34)
        context.stroke()

        context.fillStyle = 'rgba(223, 255, 220, 0.16)'
        this.drawRoundedRect(context, 72, 82, 230, 58, 29)
        context.fill()

        context.fillStyle = '#eaffd7'
        context.font = '700 26px Arial'
        context.fillText('GUIDED WELCOME', 104, 120)

        context.fillStyle = '#ffffff'
        context.font = '700 58px Arial'
        context.fillText('Welcome to Green Roots World', 78, 214)

        context.fillStyle = '#d7ffd1'
        context.font = '600 31px Arial'
        this.drawWrappedText(
            context,
            'Enjoy a guided ride through a living farm universe where stories, innovation, and services grow together around you.',
            82,
            288,
            860,
            42
        )

        context.fillStyle = '#f5ffe8'
        context.font = '500 25px Arial'
        this.drawWrappedText(
            context,
            'Stay with the road, discover each scene, and open every module whenever you are ready.',
            82,
            402,
            850,
            36
        )

        context.fillStyle = '#b8ff93'
        context.beginPath()
        context.arc(882, 126, 32, 0, Math.PI * 2)
        context.fill()

        context.fillStyle = '#114321'
        context.font = '700 34px Arial'
        context.fillText('GO', 858, 138)

        return new THREE.CanvasTexture(canvas)
    }

    drawRoundedRect(_context, _x, _y, _width, _height, _radius)
    {
        _context.beginPath()
        _context.moveTo(_x + _radius, _y)
        _context.lineTo(_x + _width - _radius, _y)
        _context.quadraticCurveTo(_x + _width, _y, _x + _width, _y + _radius)
        _context.lineTo(_x + _width, _y + _height - _radius)
        _context.quadraticCurveTo(_x + _width, _y + _height, _x + _width - _radius, _y + _height)
        _context.lineTo(_x + _radius, _y + _height)
        _context.quadraticCurveTo(_x, _y + _height, _x, _y + _height - _radius)
        _context.lineTo(_x, _y + _radius)
        _context.quadraticCurveTo(_x, _y, _x + _radius, _y)
        _context.closePath()
    }

    drawWrappedText(_context, _text, _x, _y, _maxWidth, _lineHeight)
    {
        const words = _text.split(' ')
        let line = ''
        let lineIndex = 0

        for(const word of words)
        {
            const testLine = line ? `${line} ${word}` : word
            if(_context.measureText(testLine).width > _maxWidth && line)
            {
                _context.fillText(line, _x, _y + lineIndex * _lineHeight)
                line = word
                lineIndex++
            }
            else
            {
                line = testLine
            }
        }

        if(line)
        {
            _context.fillText(line, _x, _y + lineIndex * _lineHeight)
        }
    }

    setSounds()
    {
        this.sounds = new Sounds({
            debug: this.debugFolder,
            time: this.time
        })
    }

    setAxes()
    {
        this.axis = new THREE.AxesHelper()
        this.container.add(this.axis)
    }

    setControls()
    {
        this.controls = new Controls({
            config: this.config,
            sizes: this.sizes,
            time: this.time,
            camera: this.camera,
            sounds: this.sounds
        })
    }

    setMaterials()
    {
        this.materials = new Materials({
            resources: this.resources,
            debug: this.debugFolder
        })
    }

    setFloor()
    {
        this.floor = new Floor({
            debug: this.debugFolder
        })

        this.container.add(this.floor.container)
    }

    setShadows()
    {
        this.shadows = new Shadows({
            time: this.time,
            debug: this.debugFolder,
            renderer: this.renderer,
            camera: this.camera
        })
        this.container.add(this.shadows.container)
    }

    setPhysics()
    {
        this.physics = new Physics({
            config: this.config,
            debug: this.debug,
            scene: this.scene,
            time: this.time,
            sizes: this.sizes,
            controls: this.controls,
            sounds: this.sounds
        })

        this.container.add(this.physics.models.container)
    }

    setZones()
    {
        this.zones = new Zones({
            time: this.time,
            physics: this.physics,
            debug: this.debugFolder
        })
        this.container.add(this.zones.container)
    }

    setAreas()
    {
        this.areas = new Areas({
            config: this.config,
            resources: this.resources,
            debug: this.debug,
            renderer: this.renderer,
            camera: this.camera,
            car: this.car,
            sounds: this.sounds,
            time: this.time
        })

        this.container.add(this.areas.container)
    }

    setTiles()
    {
        this.tiles = new Tiles({
            resources: this.resources,
            objects: this.objects,
            debug: this.debug
        })
    }

    setWalls()
    {
        this.walls = new Walls({
            resources: this.resources,
            objects: this.objects
        })
    }

    setObjects()
    {
        this.objects = new Objects({
            time: this.time,
            resources: this.resources,
            materials: this.materials,
            physics: this.physics,
            shadows: this.shadows,
            sounds: this.sounds,
            debug: this.debugFolder
        })
        this.container.add(this.objects.container)

        // window.requestAnimationFrame(() =>
        // {
        //     this.objects.merge.update()
        // })
    }

    setCar()
    {
        this.car = new Car({
            time: this.time,
            resources: this.resources,
            objects: this.objects,
            physics: this.physics,
            shadows: this.shadows,
            materials: this.materials,
            controls: this.controls,
            sounds: this.sounds,
            renderer: this.renderer,
            camera: this.camera,
            debug: this.debugFolder,
            config: this.config
        })
        this.container.add(this.car.container)
    }

    setSections()
    {
        this.sections = {}

        // Generic options
        const options = {
            config: this.config,
            time: this.time,
            resources: this.resources,
            camera: this.camera,
            passes: this.passes,
            objects: this.objects,
            areas: this.areas,
            zones: this.zones,
            walls: this.walls,
            tiles: this.tiles,
            debug: this.debugFolder
        }

        // // Distinction A
        // this.sections.distinctionA = new DistinctionASection({
        //     ...options,
        //     x: 0,
        //     y: - 15
        // })
        // this.container.add(this.sections.distinctionA.container)

        // // Distinction B
        // this.sections.distinctionB = new DistinctionBSection({
        //     ...options,
        //     x: 0,
        //     y: - 15
        // })
        // this.container.add(this.sections.distinctionB.container)

        // // Distinction C
        // this.sections.distinctionC = new DistinctionCSection({
        //     ...options,
        //     x: 0,
        //     y: 0
        // })
        // this.container.add(this.sections.distinctionC.container)

        // // Distinction D
        // this.sections.distinctionD = new DistinctionDSection({
        //     ...options,
        //     x: 0,
        //     y: 0
        // })
        // this.container.add(this.sections.distinctionD.container)

        // Intro
        this.sections.intro = new IntroSection({
            ...options,
            x: 0,
            y: 0
        })
        this.container.add(this.sections.intro.container)

        // Crossroads
        this.sections.crossroads = new CrossroadsSection({
            ...options,
            x: 0,
            y: - 30
        })
        this.container.add(this.sections.crossroads.container)

        // Projects
        this.sections.projects = new ProjectsSection({
            ...options,
            x: 30,
            y: - 30
            // x: 0,
            // y: 0
        })
        this.container.add(this.sections.projects.container)

        // Information
        this.sections.information = new InformationSection({
            ...options,
            x: 1.2,
            y: - 55
            // x: 0,
            // y: - 10
        })
        this.container.add(this.sections.information.container)

        // Playground
        this.sections.playground = new PlaygroundSection({
            ...options,
            x: - 38,
            y: - 34
            // x: - 15,
            // y: - 4
        })
        this.container.add(this.sections.playground.container)
    }

    setTrees()
    {
        this.treesSystem = new Trees({
            time:      this.time,
            materials: this.materials,
            container: this.container
        })
    }

    setEasterEggs()
    {
        this.easterEggs = new EasterEggs({
            resources: this.resources,
            car: this.car,
            walls: this.walls,
            objects: this.objects,
            materials: this.materials,
            areas: this.areas,
            config: this.config,
            physics: this.physics
        })
        this.container.add(this.easterEggs.container)
    }

    setAutopilot()
    {
        this.autopilot = new Autopilot({
            time:     this.time,
            car:      this.car,
            controls: this.controls,
            sections: this.sections
        })
    }
}
