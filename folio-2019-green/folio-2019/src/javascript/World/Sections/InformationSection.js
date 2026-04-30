import * as THREE from 'three'

export default class InformationSection {
    constructor(_options) {
        // Options
        this.time = _options.time
        this.resources = _options.resources
        this.objects = _options.objects
        this.areas = _options.areas
        this.tiles = _options.tiles
        this.debug = _options.debug
        this.x = _options.x
        this.y = _options.y

        // Set up
        this.container = new THREE.Object3D()
        this.container.matrixAutoUpdate = false

        this.setStatic()
        this.setLinks()
        this.setActivities()
        this.setExplorerHub()
        this.setTiles()
    }

    setStatic() {
        this.static = this.objects.add({
            base: this.resources.items.informationStaticBase.scene,
            collision: this.resources.items.informationStaticCollision.scene,
            floorShadowTexture: this.resources.items.informationStaticFloorShadowTexture,
            offset: new THREE.Vector3(this.x, this.y, 0),
            mass: 0
        })

        // Force-refresh all local matrices (positions were shifted by collision.center
        // after the initial updateMatrix() call, leaving local matrices stale).
        this.static.container.updateMatrix()
        this.static.container.traverse((child) => { child.updateMatrix() })
        this.static.container.updateMatrixWorld(true)

        // Collect shadeOrange statues for removal — record their exact world positions
        // so we can place the replacement agriculture models in the same spots.
        this.orangeStatuePositions = []
        const toRemove = []

        this.static.container.traverse((child) => {
            if (!child.isMesh) {
                return
            }

            const worldPosition = new THREE.Vector3()
            child.getWorldPosition(worldPosition)
            const localX = worldPosition.x - this.x
            const localY = worldPosition.y - this.y
            const localZ = worldPosition.z

            // Hide legacy Eiffel tower / French flag area.
            if (localX > - 7.2 && localX < - 2.6 && localY > 3.6 && localY < 7.2) {
                child.visible = false
            }

            // Hide the original left podium props visible near the INFORMATION sign.
            if (localX > - 9.8 && localX < - 6.8 && localY > - 0.2 && localY < 2.2 && localZ > 0.45) {
                child.visible = false
            }

            // Remove the five orange Blender humanoid statues — visibility=false alone
            // is not reliable, so we physically detach them from the scene graph.
            if (/^shadeOrange/i.test(child.name)) {
                this.orangeStatuePositions.push(worldPosition.clone())
                toRemove.push(child)
            }
        })

        for (const child of toRemove) {
            if (child.parent) {
                child.parent.remove(child)
            }
        }

        this.setTunisianFlag()
    }

    setTunisianFlag() {
        this.flag = {}
        this.flag.container = new THREE.Object3D()
        this.flag.container.position.set(this.x - 4.35, this.y + 5.25, 0)
        this.flag.container.matrixAutoUpdate = false
        this.flag.container.updateMatrix()
        this.container.add(this.flag.container)

        this.flag.pole = new THREE.Mesh(
            new THREE.BoxGeometry(0.12, 0.12, 3.4),
            new THREE.MeshBasicMaterial({ color: 0xf1f1f1 })
        )
        this.flag.pole.position.set(0, 0, 1.7)
        this.flag.pole.matrixAutoUpdate = false
        this.flag.pole.updateMatrix()
        this.flag.container.add(this.flag.pole)

        this.flag.texture = this.createTunisianFlagTexture()
        this.flag.texture.magFilter = THREE.LinearFilter
        this.flag.texture.minFilter = THREE.LinearFilter

        this.flag.cloth = new THREE.Mesh(
            new THREE.PlaneGeometry(1.9, 1.2),
            new THREE.MeshBasicMaterial({ map: this.flag.texture, transparent: true, side: THREE.DoubleSide })
        )
        this.flag.cloth.position.set(0.98, 0, 2.5)
        this.flag.cloth.rotation.x = Math.PI * 0.5
        this.flag.cloth.matrixAutoUpdate = false
        this.flag.cloth.updateMatrix()
        this.flag.container.add(this.flag.cloth)
    }

    createTunisianFlagTexture() {
        const canvas = document.createElement('canvas')
        canvas.width = 1200
        canvas.height = 800
        const context = canvas.getContext('2d')

        if (!context) {
            return new THREE.CanvasTexture(canvas)
        }

        const red = '#d71920'

        context.clearRect(0, 0, canvas.width, canvas.height)
        context.fillStyle = red
        context.fillRect(0, 0, canvas.width, canvas.height)

        // White central disk
        context.fillStyle = '#ffffff'
        context.beginPath()
        context.arc(canvas.width * 0.5, canvas.height * 0.5, 185, 0, Math.PI * 2)
        context.fill()

        // Red crescent
        context.fillStyle = red
        context.beginPath()
        context.arc(canvas.width * 0.47, canvas.height * 0.5, 108, Math.PI * 0.24, Math.PI * 1.76)
        context.arc(canvas.width * 0.5, canvas.height * 0.5, 82, Math.PI * 1.78, Math.PI * 0.22, true)
        context.closePath()
        context.fill()

        // Five-point star
        const cx = canvas.width * 0.555
        const cy = canvas.height * 0.5
        const outer = 62
        const inner = outer * 0.42
        context.beginPath()
        for (let i = 0; i < 10; i++) {
            const radius = i % 2 === 0 ? outer : inner
            const angle = - Math.PI * 0.5 + i * Math.PI / 5
            const x = cx + Math.cos(angle) * radius
            const y = cy + Math.sin(angle) * radius
            if (i === 0) {
                context.moveTo(x, y)
            }
            else {
                context.lineTo(x, y)
            }
        }
        context.closePath()
        context.fill()

        return new THREE.CanvasTexture(canvas)
    }

    setAgricultureModels() {
        this.agricultureModels = {}
        this.agricultureModels.container = new THREE.Object3D()
        this.container.add(this.agricultureModels.container)

        const builders = [
            { scale: 2.2, build: () => this.createSunflower() },
            { scale: 2.0, build: () => this.createWindTurbine() },
            { scale: 2.0, build: () => this.createGrainSilo() },
            { scale: 2.0, build: () => this.createBarn() },
            { scale: 1.8, build: () => this.createTractor() }
        ]

        // Use the exact world positions where the shadeOrange statues stood.
        // Sort left-to-right by X, deduplicate positions that are within 0.5 units.
        const rawPositions = (this.orangeStatuePositions || []).slice().sort((a, b) => a.x - b.x)
        const positions = []
        for (const pos of rawPositions) {
            if (!positions.some((p) => Math.abs(p.x - pos.x) < 0.5 && Math.abs(p.y - pos.y) < 0.5)) {
                positions.push(pos)
            }
        }

        // Pad if fewer than 5 were found (evenly extend the line)
        while (positions.length < 5 && positions.length > 0) {
            const last = positions[positions.length - 1]
            positions.push(new THREE.Vector3(last.x + 2.2, last.y, last.z))
        }

        positions.slice(0, 5).forEach((pos, i) => {
            const { scale, build } = builders[i]
            const model = build()
            // Place at the statue's world position; add a small Z offset so the
            // model sits on top of whatever surface the statue was standing on.
            model.position.set(pos.x, pos.y, pos.z + 0.05)
            model.scale.setScalar(scale)
            this.agricultureModels.container.add(model)
        })
    }

    createSunflower() {
        const group = new THREE.Object3D()

        // Tapered stem — bright green
        const stemMat = new THREE.MeshBasicMaterial({ color: 0x4a9c20 })
        const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.048, 0.82, 10), stemMat)
        stem.rotation.x = Math.PI * 0.5
        stem.position.z = 0.41
        group.add(stem)

        // 4 oval leaves
        const leafMat = new THREE.MeshBasicMaterial({ color: 0x3aaa18, side: THREE.DoubleSide })
        const leafData = [
            { ox: 0.18, oz: 0.26, rz: Math.PI * 0.38 },
            { ox: -0.18, oz: 0.26, rz: -Math.PI * 0.38 },
            { ox: 0.15, oz: 0.50, rz: Math.PI * 0.32 },
            { ox: -0.15, oz: 0.50, rz: -Math.PI * 0.32 },
        ]
        for (const ld of leafData) {
            const leaf = new THREE.Mesh(new THREE.SphereGeometry(0.15, 10, 6), leafMat)
            leaf.scale.set(1.0, 0.26, 0.44)
            leaf.position.set(ld.ox, 0, ld.oz)
            leaf.rotation.z = ld.rz
            leaf.rotation.x = Math.PI * 0.5
            group.add(leaf)
        }

        // Green calyx dome
        const calyxMat = new THREE.MeshBasicMaterial({ color: 0x3a8018 })
        const calyx = new THREE.Mesh(new THREE.SphereGeometry(0.24, 14, 10, 0, Math.PI * 2, 0, Math.PI * 0.55), calyxMat)
        calyx.rotation.x = -Math.PI * 0.5
        calyx.position.z = 0.83
        group.add(calyx)

        // Outer petals — 18 bright yellow elongated spheres
        const outerMat = new THREE.MeshBasicMaterial({ color: 0xffcc00 })
        for (let i = 0; i < 18; i++) {
            const angle = (i / 18) * Math.PI * 2
            const p = new THREE.Mesh(new THREE.SphereGeometry(0.095, 8, 5), outerMat)
            p.scale.set(0.32, 1.0, 0.20)
            p.position.set(Math.cos(angle) * 0.31, Math.sin(angle) * 0.31, 0.86)
            p.rotation.z = angle + Math.PI * 0.5
            p.rotation.x = Math.PI * 0.5
            group.add(p)
        }

        // Inner petals — golden yellow
        const innerMat = new THREE.MeshBasicMaterial({ color: 0xe8a800 })
        for (let i = 0; i < 13; i++) {
            const angle = (i / 13) * Math.PI * 2 + Math.PI / 13
            const p = new THREE.Mesh(new THREE.SphereGeometry(0.072, 8, 4), innerMat)
            p.scale.set(0.28, 0.80, 0.16)
            p.position.set(Math.cos(angle) * 0.23, Math.sin(angle) * 0.23, 0.86)
            p.rotation.z = angle + Math.PI * 0.5
            p.rotation.x = Math.PI * 0.5
            group.add(p)
        }

        // Center disk — rich brown cylinder
        const diskMat = new THREE.MeshBasicMaterial({ color: 0x5c2e08 })
        const disk = new THREE.Mesh(new THREE.CylinderGeometry(0.19, 0.21, 0.10, 20), diskMat)
        disk.rotation.x = Math.PI * 0.5
        disk.position.z = 0.86
        group.add(disk)

        // Seed rings — alternating brown/tan
        const s1 = new THREE.MeshBasicMaterial({ color: 0x4a2208 })
        const s2 = new THREE.MeshBasicMaterial({ color: 0xc09050 })
        for (let ring = 0; ring < 4; ring++) {
            const r = ring * 0.044
            const count = ring === 0 ? 1 : Math.round(ring * 6.3)
            const sMat = ring % 2 === 0 ? s1 : s2
            for (let s = 0; s < count; s++) {
                const ang = (s / count) * Math.PI * 2
                const bump = new THREE.Mesh(new THREE.SphereGeometry(0.016, 6, 4), sMat)
                bump.position.set(Math.cos(ang) * r, Math.sin(ang) * r, 0.92)
                group.add(bump)
            }
        }

        return group
    }

    createWindTurbine() {
        const group = new THREE.Object3D()

        // Foundation pad
        const foundMat = new THREE.MeshBasicMaterial({ color: 0xa8a490 })
        const found = new THREE.Mesh(new THREE.CylinderGeometry(0.20, 0.22, 0.055, 8), foundMat)
        found.rotation.x = Math.PI * 0.5
        found.position.z = 0.028
        group.add(found)

        // Tapered tower — warm light gray
        const towerMat = new THREE.MeshBasicMaterial({ color: 0xdedad0 })
        const tower = new THREE.Mesh(new THREE.CylinderGeometry(0.048, 0.130, 0.86, 14), towerMat)
        tower.rotation.x = Math.PI * 0.5
        tower.position.z = 0.43
        group.add(tower)

        // Nacelle — medium gray box, oriented along Y (faces viewer)
        const nacelMat = new THREE.MeshBasicMaterial({ color: 0xb8b4a8 })
        const nacel = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.28, 0.12), nacelMat)
        nacel.position.set(0, 0.06, 0.92)
        group.add(nacel)
        // Nacelle tail fin (vertical)
        const finMat = new THREE.MeshBasicMaterial({ color: 0xa0a090 })
        const fin = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.022, 0.14), finMat)
        fin.position.set(0, -0.14, 0.97)
        group.add(fin)

        // Hub — prominent sphere, facing viewer (+Y front)
        const hubMat = new THREE.MeshBasicMaterial({ color: 0x909080 })
        const hub = new THREE.Mesh(new THREE.SphereGeometry(0.060, 12, 9), hubMat)
        hub.position.set(0, 0.22, 0.92)
        group.add(hub)

        // Rotor spins around Y axis — blades extend in Z (vertical circle facing +Y)
        const rotor = new THREE.Object3D()
        rotor.position.set(0, 0.22, 0.92)
        group.add(rotor)

        // 3 blades: thick enough to be visible, blue-gray color
        const bladeMat = new THREE.MeshBasicMaterial({ color: 0x7aaac8 })
        const bladeRootMat = new THREE.MeshBasicMaterial({ color: 0x5a8aaa })
        for (let i = 0; i < 3; i++) {
            const bg = new THREE.Object3D()
            bg.rotation.y = (i / 3) * Math.PI * 2
            rotor.add(bg)

            // Root block (wide)
            const root = new THREE.Mesh(new THREE.BoxGeometry(0.055, 0.038, 0.068), bladeRootMat)
            root.position.set(0, 0, 0.048)
            bg.add(root)
            // Mid section
            const mid = new THREE.Mesh(new THREE.BoxGeometry(0.040, 0.030, 0.22), bladeMat)
            mid.position.set(0, 0, 0.19)
            bg.add(mid)
            // Tip section (narrower)
            const tip = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.022, 0.18), bladeMat)
            tip.position.set(0, 0, 0.39)
            bg.add(tip)
            // Tip cap sphere
            const tipCap = new THREE.Mesh(new THREE.SphereGeometry(0.016, 7, 5), bladeRootMat)
            tipCap.position.set(0, 0, 0.49)
            bg.add(tipCap)
        }

        group.userData.rotor = rotor
        return group
    }

    createGrainSilo() {
        const group = new THREE.Object3D()

        const siloMatA = new THREE.MeshBasicMaterial({ color: 0xddd4b0 })
        const siloMatB = new THREE.MeshBasicMaterial({ color: 0xcec598 })
        const roofMat = new THREE.MeshBasicMaterial({ color: 0x8a6040 })
        const ribMat = new THREE.MeshBasicMaterial({ color: 0xb8a870 })
        const baseMat = new THREE.MeshBasicMaterial({ color: 0x989080 })

        // Two silo cylinders side by side
        for (const { ox, mat } of [{ ox: -0.2, mat: siloMatA }, { ox: 0.2, mat: siloMatB }]) {
            const body = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.19, 0.80, 16), mat)
            body.rotation.x = Math.PI * 0.5
            body.position.set(ox, 0, 0.40)
            group.add(body)

            const roof = new THREE.Mesh(new THREE.ConeGeometry(0.205, 0.26, 16), roofMat)
            roof.rotation.x = Math.PI * 0.5
            roof.position.set(ox, 0, 0.93)
            group.add(roof)

            for (let r = 0; r < 5; r++) {
                const rib = new THREE.Mesh(new THREE.TorusGeometry(0.188, 0.014, 6, 16), ribMat)
                rib.rotation.x = Math.PI * 0.5
                rib.position.set(ox, 0, 0.10 + r * 0.15)
                group.add(rib)
            }

            const base = new THREE.Mesh(new THREE.CylinderGeometry(0.20, 0.22, 0.05, 16), baseMat)
            base.rotation.x = Math.PI * 0.5
            base.position.set(ox, 0, 0.025)
            group.add(base)
        }

        // Connecting pipe between silos
        const pipeMat = new THREE.MeshBasicMaterial({ color: 0x8a8070 })
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.038, 0.038, 0.40, 8), pipeMat)
        bridge.rotation.z = Math.PI * 0.5
        bridge.position.z = 0.56
        group.add(bridge)

        // Ladder — two rails + 7 rungs
        const ladderMat = new THREE.MeshBasicMaterial({ color: 0x707070 })
        const railL = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.018, 0.78), ladderMat)
        railL.position.set(0.407, -0.035, 0.40)
        group.add(railL)
        const railR = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.018, 0.78), ladderMat)
        railR.position.set(0.407, 0.035, 0.40)
        group.add(railR)
        for (let r = 0; r < 7; r++) {
            const rung = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.082, 0.015), ladderMat)
            rung.position.set(0.410, 0, 0.06 + r * 0.10)
            group.add(rung)
        }

        // Access door on left silo
        const doorMat = new THREE.MeshBasicMaterial({ color: 0x8a6030 })
        const door = new THREE.Mesh(new THREE.BoxGeometry(0.07, 0.03, 0.17), doorMat)
        door.position.set(-0.2, -0.196, 0.085)
        group.add(door)

        return group
    }

    createBarn() {
        const group = new THREE.Object3D()

        // Concrete foundation
        const foundMat = new THREE.MeshBasicMaterial({ color: 0xb0a898 })
        const found = new THREE.Mesh(new THREE.BoxGeometry(0.84, 0.54, 0.055), foundMat)
        found.position.z = 0.028
        group.add(found)

        // Main barn body — vivid red
        const barnMat = new THREE.MeshBasicMaterial({ color: 0xc82010 })
        const body = new THREE.Mesh(new THREE.BoxGeometry(0.78, 0.48, 0.50), barnMat)
        body.position.z = 0.305
        group.add(body)

        // White corner trim
        const trimMat = new THREE.MeshBasicMaterial({ color: 0xf2efe2 })
        for (const cx of [-0.39, 0.39]) {
            for (const cy of [-0.24, 0.24]) {
                const corner = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.026, 0.52), trimMat)
                corner.position.set(cx, cy, 0.31)
                group.add(corner)
            }
        }
        // White horizontal band
        const band = new THREE.Mesh(new THREE.BoxGeometry(0.80, 0.50, 0.018), trimMat)
        band.position.z = 0.30
        group.add(band)

        // Gabled roof — warm BROWN (visible!) — two angled panels + ridge
        const roofMat = new THREE.MeshBasicMaterial({ color: 0x8b4a1e })
        const roofL = new THREE.Mesh(new THREE.BoxGeometry(0.49, 0.51, 0.048), roofMat)
        roofL.rotation.y = -0.58
        roofL.position.set(-0.19, 0, 0.635)
        group.add(roofL)
        const roofR = new THREE.Mesh(new THREE.BoxGeometry(0.49, 0.51, 0.048), roofMat)
        roofR.rotation.y = 0.58
        roofR.position.set(0.19, 0, 0.635)
        group.add(roofR)
        const ridge = new THREE.Mesh(new THREE.BoxGeometry(0.058, 0.52, 0.058), roofMat)
        ridge.position.z = 0.835
        group.add(ridge)

        // Sliding door with X-brace
        const doorMat = new THREE.MeshBasicMaterial({ color: 0x6a4020 })
        const doorFace = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.022, 0.30), doorMat)
        doorFace.position.set(0, -0.242, 0.155)
        group.add(doorFace)
        const braceA = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.022, 0.37), doorMat)
        braceA.rotation.x = 0.84
        braceA.position.set(0, -0.242, 0.155)
        group.add(braceA)
        const braceB = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.022, 0.37), doorMat)
        braceB.rotation.x = -0.84
        braceB.position.set(0, -0.242, 0.155)
        group.add(braceB)

        // Hayloft window + frame
        const glassMat = new THREE.MeshBasicMaterial({ color: 0x50a0c8 })
        const loftWin = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.022, 0.10), glassMat)
        loftWin.position.set(0, -0.242, 0.43)
        group.add(loftWin)
        const loftFrame = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.025, 0.12), trimMat)
        loftFrame.position.set(0, -0.244, 0.43)
        group.add(loftFrame)

        // Side windows
        for (const wz of [0.26, 0.40]) {
            const sideWin = new THREE.Mesh(new THREE.BoxGeometry(0.10, 0.022, 0.10), glassMat)
            sideWin.position.set(0.395, 0, wz)
            group.add(sideWin)
        }

        // Ventilation cupola — medium brown (visible!)
        const cupolaMat = new THREE.MeshBasicMaterial({ color: 0x7a4220 })
        const cupola = new THREE.Mesh(new THREE.BoxGeometry(0.10, 0.10, 0.10), cupolaMat)
        cupola.position.z = 0.885
        group.add(cupola)
        const cupolaRoof = new THREE.Mesh(new THREE.ConeGeometry(0.082, 0.10, 4), cupolaMat)
        cupolaRoof.rotation.x = Math.PI * 0.5
        cupolaRoof.rotation.z = Math.PI * 0.25
        cupolaRoof.position.z = 0.985
        group.add(cupolaRoof)

        return group
    }

    createTractor() {
        const group = new THREE.Object3D()

        // Bright, saturated green palette
        const frameMat = new THREE.MeshBasicMaterial({ color: 0x2e7e28 })
        const bodyMat = new THREE.MeshBasicMaterial({ color: 0x38a030 })
        const darkMat = new THREE.MeshBasicMaterial({ color: 0x245c20 })
        const cabMat = new THREE.MeshBasicMaterial({ color: 0x40b838 })
        const glassMat = new THREE.MeshBasicMaterial({ color: 0x60c0dc })
        const tireMat = new THREE.MeshBasicMaterial({ color: 0x303030 })
        const silverMat = new THREE.MeshBasicMaterial({ color: 0xd0d0c0 })
        const greyMat = new THREE.MeshBasicMaterial({ color: 0x909090 })
        const fenderMat = new THREE.MeshBasicMaterial({ color: 0x2c6822 })
        const yellowMat = new THREE.MeshBasicMaterial({ color: 0xffe040 })

        // Main chassis
        const chassis = new THREE.Mesh(new THREE.BoxGeometry(0.70, 0.22, 0.14), frameMat)
        chassis.position.z = 0.22
        group.add(chassis)

        // Engine hood
        const hoodBase = new THREE.Mesh(new THREE.BoxGeometry(0.29, 0.25, 0.22), bodyMat)
        hoodBase.position.set(0.245, 0, 0.40)
        group.add(hoodBase)
        const hoodTop = new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.21, 0.08), bodyMat)
        hoodTop.position.set(0.22, 0, 0.52)
        group.add(hoodTop)

        // Front grill — dark gray (not pure black)
        const grillMat = new THREE.MeshBasicMaterial({ color: 0x444444 })
        const grill = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.21, 0.17), grillMat)
        grill.position.set(0.387, 0, 0.405)
        group.add(grill)
        for (let g = 0; g < 4; g++) {
            const bar = new THREE.Mesh(new THREE.BoxGeometry(0.030, 0.19, 0.013), silverMat)
            bar.position.set(0.385, 0, 0.335 + g * 0.040)
            group.add(bar)
        }

        // Headlights — bright yellow
        for (const hy of [-0.08, 0.08]) {
            const light = new THREE.Mesh(new THREE.CylinderGeometry(0.024, 0.024, 0.024, 8), yellowMat)
            light.rotation.z = Math.PI * 0.5
            light.position.set(0.392, hy, 0.465)
            group.add(light)
        }

        // Cab body
        const cab = new THREE.Mesh(new THREE.BoxGeometry(0.33, 0.31, 0.31), cabMat)
        cab.position.set(-0.10, 0, 0.545)
        group.add(cab)

        // Cab roof — darker green for contrast
        const cabRoof = new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.33, 0.042), darkMat)
        cabRoof.position.set(-0.10, 0, 0.71)
        group.add(cabRoof)

        // Windows — sky blue
        const frontWin = new THREE.Mesh(new THREE.BoxGeometry(0.022, 0.23, 0.21), glassMat)
        frontWin.position.set(0.075, 0, 0.545)
        group.add(frontWin)
        const rearWin = new THREE.Mesh(new THREE.BoxGeometry(0.022, 0.23, 0.21), glassMat)
        rearWin.position.set(-0.275, 0, 0.545)
        group.add(rearWin)
        for (const wy of [-0.155, 0.155]) {
            const sideWin = new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.022, 0.19), glassMat)
            sideWin.position.set(-0.10, wy, 0.545)
            group.add(sideWin)
        }

        // Exhaust stack — warm gray
        const stackBase = new THREE.Mesh(new THREE.CylinderGeometry(0.019, 0.027, 0.30, 8), greyMat)
        stackBase.rotation.x = Math.PI * 0.5
        stackBase.position.set(0.15, -0.12, 0.65)
        group.add(stackBase)
        const stackCap = new THREE.Mesh(new THREE.CylinderGeometry(0.034, 0.019, 0.042, 8), greyMat)
        stackCap.rotation.x = Math.PI * 0.5
        stackCap.position.set(0.15, -0.12, 0.805)
        group.add(stackCap)

        // Rear large wheels — dark gray tires
        for (const ry of [0.23, -0.23]) {
            const tire = new THREE.Mesh(new THREE.TorusGeometry(0.215, 0.085, 10, 20), tireMat)
            tire.rotation.x = Math.PI * 0.5
            tire.position.set(-0.18, ry, 0.215)
            group.add(tire)

            for (let t = 0; t < 8; t++) {
                const ang = (t / 8) * Math.PI * 2
                const tread = new THREE.Mesh(new THREE.BoxGeometry(0.088, 0.058, 0.022), greyMat)
                tread.position.set(
                    -0.18 + Math.cos(ang) * 0.215,
                    ry,
                    0.215 + Math.sin(ang) * 0.215
                )
                tread.rotation.y = ang
                group.add(tread)
            }

            const rim = new THREE.Mesh(new THREE.CylinderGeometry(0.122, 0.122, 0.075, 14), silverMat)
            rim.rotation.x = Math.PI * 0.5
            rim.position.set(-0.18, ry, 0.215)
            group.add(rim)

            const hubC = new THREE.Mesh(new THREE.SphereGeometry(0.040, 8, 6), greyMat)
            hubC.position.set(-0.18, ry, 0.215)
            group.add(hubC)

            // Fender arc
            const fender = new THREE.Mesh(new THREE.TorusGeometry(0.238, 0.030, 6, 12, Math.PI * 0.72), fenderMat)
            fender.rotation.x = Math.PI * 0.5
            fender.rotation.z = Math.PI * 0.36
            fender.position.set(-0.18, ry, 0.215)
            group.add(fender)
        }

        // Front small wheels
        for (const fy of [0.148, -0.148]) {
            const tire = new THREE.Mesh(new THREE.TorusGeometry(0.11, 0.048, 8, 14), tireMat)
            tire.rotation.x = Math.PI * 0.5
            tire.position.set(0.265, fy, 0.11)
            group.add(tire)
            const rim = new THREE.Mesh(new THREE.CylinderGeometry(0.056, 0.056, 0.048, 10), silverMat)
            rim.rotation.x = Math.PI * 0.5
            rim.position.set(0.265, fy, 0.11)
            group.add(rim)
        }

        // Three-point hitch
        const hitchMat = new THREE.MeshBasicMaterial({ color: 0x707070 })
        const hitchBar = new THREE.Mesh(new THREE.BoxGeometry(0.062, 0.31, 0.023), hitchMat)
        hitchBar.position.set(-0.385, 0, 0.24)
        group.add(hitchBar)
        for (const hy of [0.12, -0.12]) {
            const arm = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.017, 0.017), hitchMat)
            arm.position.set(-0.45, hy, 0.18)
            group.add(arm)
        }

        return group
    }

    setLinks() {
        // Set up
        this.links = {}
        this.links.x = 1.95
        this.links.y = - 1.5
        this.links.halfExtents = {}
        this.links.halfExtents.x = 1
        this.links.halfExtents.y = 1
        this.links.distanceBetween = 2.4
        this.links.labelWidth = this.links.halfExtents.x * 2 + 1
        this.links.labelGeometry = new THREE.PlaneGeometry(this.links.labelWidth, this.links.labelWidth * 0.25, 1, 1)
        this.links.labelOffset = - 1.6
        this.links.items = []

        this.links.container = new THREE.Object3D()
        this.links.container.matrixAutoUpdate = false
        this.container.add(this.links.container)

        // Options
        this.links.options = [
            {
                href: 'https://github.com/labidi-houssem/Gestion-User-Pi-Cloud',
                labelTexture: this.createTextTexture('Repo User')
            },
            {
                href: 'https://github.com/Aminraissi/PI',
                labelTexture: this.createTextTexture('PI Github')
            },
            {
                href: 'https://github.com/labidi-houssem/pi-cloud',
                labelTexture: this.createTextTexture('Repo Backend')
            },
            {
                href: 'https://github.com/ghadamaalej/AgricultureFront',
                labelTexture: this.createTextTexture('Repo Frontend')
            }
        ]

        // Create each link
        let i = 0
        for (const _option of this.links.options) {
            // Set up
            const item = {}
            item.x = this.x + this.links.x + this.links.distanceBetween * i
            item.y = this.y + this.links.y
            item.href = _option.href

            // Create area
            item.area = this.areas.add({
                position: new THREE.Vector2(item.x, item.y),
                halfExtents: new THREE.Vector2(this.links.halfExtents.x, this.links.halfExtents.y)
            })
            item.area.on('interact', () => {
                window.open(_option.href, '_blank')
                // Deactivate this area briefly so the car-position tick
                // cannot immediately re-trigger in() while the car is still on top of it
                item.area.deactivate()
                window.setTimeout(() => {
                    item.area.activate()
                }, 2000)
            })

            // Texture
            item.texture = _option.labelTexture
            item.texture.magFilter = THREE.NearestFilter
            item.texture.minFilter = THREE.LinearFilter

            // Create label
            item.labelMesh = new THREE.Mesh(this.links.labelGeometry, new THREE.MeshBasicMaterial({ wireframe: false, color: 0xffffff, alphaMap: _option.labelTexture, depthTest: true, depthWrite: false, transparent: true }))
            item.labelMesh.position.x = item.x + this.links.labelWidth * 0.5 - this.links.halfExtents.x
            item.labelMesh.position.y = item.y + this.links.labelOffset
            item.labelMesh.matrixAutoUpdate = false
            item.labelMesh.updateMatrix()
            this.links.container.add(item.labelMesh)

            // Save
            this.links.items.push(item)

            i++
        }
    }

    createTextTexture(_text) {
        const canvas = document.createElement('canvas')
        canvas.width = 1024
        canvas.height = 256
        const context = canvas.getContext('2d')

        if(!context) {
            return new THREE.CanvasTexture(canvas)
        }

        context.clearRect(0, 0, canvas.width, canvas.height)
        context.fillStyle = '#ffffff'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        
        let fontSize = 96
        const textToDraw = _text.toUpperCase()
        
        do {
            context.font = `bold ${fontSize}px "Arial Black", Arial, sans-serif`
            if (context.measureText(textToDraw).width <= canvas.width * 0.9) {
                break
            }
            fontSize -= 4
        } while (fontSize > 20)

        context.fillText(textToDraw, canvas.width / 2, canvas.height / 2)

        const texture = new THREE.CanvasTexture(canvas)
        texture.magFilter = THREE.LinearFilter
        texture.minFilter = THREE.LinearFilter
        texture.needsUpdate = true

        return texture
    }

    setActivities() {
        // Set up
        this.activities = {}
        this.activities.x = this.x + 0
        this.activities.y = this.y - 10
        this.activities.multiplier = 5.5

        // Geometry
        this.activities.geometry = new THREE.PlaneGeometry(2 * this.activities.multiplier, 1 * this.activities.multiplier, 1, 1)

        // Texture
        this.activities.texture = this.resources.items.informationActivitiesTexture
        this.activities.texture.magFilter = THREE.NearestFilter
        this.activities.texture.minFilter = THREE.LinearFilter

        // Material
        this.activities.material = new THREE.MeshBasicMaterial({ wireframe: false, color: 0xffffff, alphaMap: this.activities.texture, transparent: true })

        // Mesh
        this.activities.mesh = new THREE.Mesh(this.activities.geometry, this.activities.material)
        this.activities.mesh.position.x = this.activities.x
        this.activities.mesh.position.y = this.activities.y
        this.activities.mesh.matrixAutoUpdate = false
        this.activities.mesh.updateMatrix()
        this.container.add(this.activities.mesh)
    }

    setExplorerHub() {
        this.explorerHub = {}
        this.explorerHub.routes = [
            { label: 'Delivery', route: '/delivery' },
            { label: 'Inventory', route: '/inventory' },
            { label: 'Appointments', route: '/appointments' },
            { label: 'Animals', route: '/animals' },
            { label: 'Marketplace', route: '/marketplace' },
            { label: 'Forum', route: '/forum' },
            { label: 'Loans', route: '/loans' },
            { label: 'Events', route: '/events' },
            { label: 'Trainings', route: '/formations' },
            { label: 'Help Request', route: '/help-request' }
        ]
        this.explorerHub.colors = [
            '#d9ff8f',
            '#f2c14e',
            '#8ed6ff',
            '#ffb77d',
            '#8ff0c6',
            '#f29e9e',
            '#c8b6ff',
            '#f6f49d',
            '#89c2ff',
            '#f7aef8',
            '#9fe870',
            '#ffd166'
        ]

        this.explorerHub.layout = {
            startX: 304,
            driveY: - 30,
            cardY: - 24.8,
            spacingX: 10.5,
            boardWidth: 4.2,
            boardHeight: 2.55,
            boardZ: 2.45,
            areaHalfExtents: new THREE.Vector2(2.2, 1.45),
            signPosition: new THREE.Vector3(294, - 20.8, 0),
            signWidth: 8.1,
            signHeight: 2.5,
            signZ: 3.55,
            signPoleHeight: 4.1,
            signPoleZ: 1.95,
            postHeight: 2.5,
            postZ: 1.18,
            plateWidth: 3.5,
            plateHeight: 1.9
        }
        this.explorerHub.container = new THREE.Object3D()
        this.explorerHub.container.matrixAutoUpdate = false
        this.explorerHub.container.updateMatrix()
        this.container.add(this.explorerHub.container)

        this.setExplorerHubPath()
        this.setExplorerHubSign()
        this.setExplorerHubRoutes()
    }

    setExplorerHubPath() {
        const pathStartX = 284
        const pathLength = (this.explorerHub.routes.length - 1) * this.explorerHub.layout.spacingX + 112

        this.tiles.add({
            start: new THREE.Vector2(pathStartX, this.explorerHub.layout.driveY),
            delta: new THREE.Vector2(pathLength, 0)
        })
    }

    setExplorerHubSign() {
        const signGroup = new THREE.Object3D()
        signGroup.position.copy(this.explorerHub.layout.signPosition)
        signGroup.matrixAutoUpdate = false

        const signTexture = this.createExplorerSignTexture()
        const signMaterial = new THREE.MeshBasicMaterial({
            map: signTexture,
            transparent: true,
            depthWrite: false,
            side: THREE.DoubleSide
        })

        const sign = new THREE.Mesh(new THREE.PlaneGeometry(this.explorerHub.layout.signWidth, this.explorerHub.layout.signHeight), signMaterial)
        sign.rotation.x = Math.PI * 0.5
        sign.position.set(0, 0, this.explorerHub.layout.signZ)
        signGroup.add(sign)

        const poleMaterial = new THREE.MeshBasicMaterial({ color: 0x9a5528 })
        const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.11, this.explorerHub.layout.signPoleHeight, 8), poleMaterial)
        pole.rotation.x = Math.PI * 0.5
        pole.position.set(- 0.18, 0, this.explorerHub.layout.signPoleZ)
        signGroup.add(pole)

        const stoneMaterial = new THREE.MeshBasicMaterial({ color: 0xf3e4ca })
        const stones = [
            { x: - 0.5, y: 0.18, z: 0.08, s: 0.15 },
            { x: 0.18, y: - 0.22, z: 0.06, s: 0.11 },
            { x: - 0.04, y: 0.28, z: 0.05, s: 0.09 }
        ]

        for (const stone of stones) {
            const mesh = new THREE.Mesh(new THREE.BoxGeometry(stone.s, stone.s * 0.85, stone.s * 0.75), stoneMaterial)
            mesh.position.set(stone.x, stone.y, stone.z)
            mesh.rotation.z = stone.x * 0.8
            signGroup.add(mesh)
        }

        signGroup.updateMatrix()
        this.explorerHub.container.add(signGroup)
    }

    setExplorerHubRoutes() {
        const { spacingX, startX, cardY, driveY, plateWidth, plateHeight } = this.explorerHub.layout

        this.explorerHub.routes.forEach((_route, index) => {
            const x = startX + index * spacingX
            const y = cardY

            const itemGroup = new THREE.Object3D()
            itemGroup.position.set(x, y, 0)
            itemGroup.matrixAutoUpdate = false

            const accentColor = this.explorerHub.colors[index % this.explorerHub.colors.length]
            const boardTexture = this.createExplorerRouteTexture(_route.label, _route.route, accentColor, index + 1)
            const boardMaterial = new THREE.MeshBasicMaterial({
                map: boardTexture,
                transparent: true,
                depthWrite: false,
                side: THREE.DoubleSide
            })

            const board = new THREE.Mesh(new THREE.PlaneGeometry(this.explorerHub.layout.boardWidth, this.explorerHub.layout.boardHeight), boardMaterial)
            board.rotation.x = Math.PI * 0.5
            board.position.set(0, 0, this.explorerHub.layout.boardZ)
            itemGroup.add(board)

            const plate = new THREE.Mesh(
                new THREE.PlaneGeometry(plateWidth, plateHeight),
                new THREE.MeshBasicMaterial({
                    color: 0xffffff,
                    transparent: true,
                    opacity: 0.06,
                    depthWrite: false,
                    side: THREE.DoubleSide
                })
            )
            plate.rotation.x = Math.PI * 0.5
            plate.position.set(0, 0, 0.03)
            itemGroup.add(plate)

            const plateOutline = new THREE.LineSegments(
                new THREE.EdgesGeometry(new THREE.PlaneGeometry(plateWidth, plateHeight)),
                new THREE.LineBasicMaterial({
                    color: 0xffffff,
                    transparent: true,
                    opacity: 0.52
                })
            )
            plateOutline.rotation.x = Math.PI * 0.5
            plateOutline.position.set(0, 0, 0.05)
            itemGroup.add(plateOutline)

            const post = new THREE.Mesh(
                new THREE.CylinderGeometry(0.05, 0.065, this.explorerHub.layout.postHeight, 8),
                new THREE.MeshBasicMaterial({ color: 0xa8602d })
            )
            post.rotation.x = Math.PI * 0.5
            post.position.set(0, 0, this.explorerHub.layout.postZ)
            itemGroup.add(post)

            const base = new THREE.Mesh(
                new THREE.CylinderGeometry(0.16, 0.19, 0.12, 10),
                new THREE.MeshBasicMaterial({ color: 0x5d3b1f })
            )
            base.rotation.x = Math.PI * 0.5
            base.position.set(0, 0, 0.06)
            itemGroup.add(base)

            itemGroup.updateMatrix()
            this.explorerHub.container.add(itemGroup)

            const area = this.areas.add({
                position: new THREE.Vector2(x, driveY),
                halfExtents: this.explorerHub.layout.areaHalfExtents,
                testCar: false
            })
            area.on('interact', () => {
                this.navigateToRoute(_route.route)
            })
        })
    }

    navigateToRoute(_route) {
        // Explicitly stop autopilot before saving, so the saved state
        // always has active=false — prevents autopilot from interfering
        // when the user returns from a module.
        if (window.application && window.application.world) {
            const world = window.application.world
            if (world.autopilot && world.autopilot.active) {
                world.autopilot.stop()
            }
            if (typeof world.saveProgress === 'function') {
                world.saveProgress({ force: true })
            }
        }

        if (window.parent && window.parent !== window) {
            window.parent.postMessage({ type: 'greenroots:navigate', route: _route }, '*')
            return
        }

        window.location.assign(_route)
    }

    createExplorerSignTexture() {
        const canvas = document.createElement('canvas')
        canvas.width = 1200
        canvas.height = 420
        const context = canvas.getContext('2d')

        if (!context) {
            return new THREE.CanvasTexture(canvas)
        }

        context.clearRect(0, 0, canvas.width, canvas.height)

        context.fillStyle = '#1d1a16'
        context.beginPath()
        context.moveTo(120, 58)
        context.lineTo(890, 58)
        context.lineTo(1060, 210)
        context.lineTo(890, 362)
        context.lineTo(120, 362)
        context.closePath()
        context.fill()

        context.fillStyle = '#f8f4e8'
        context.beginPath()
        context.moveTo(138, 76)
        context.lineTo(874, 76)
        context.lineTo(1028, 210)
        context.lineTo(874, 344)
        context.lineTo(138, 344)
        context.closePath()
        context.fill()

        context.fillStyle = '#2a7f45'
        context.beginPath()
        context.moveTo(164, 104)
        context.lineTo(842, 104)
        context.lineTo(968, 210)
        context.lineTo(842, 316)
        context.lineTo(164, 316)
        context.closePath()
        context.fill()

        context.fillStyle = '#ffffff'
        context.font = '700 118px Arial'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        context.fillText('EXPLORER', 490, 214)

        const texture = new THREE.CanvasTexture(canvas)
        texture.colorSpace = THREE.SRGBColorSpace
        texture.minFilter = THREE.LinearFilter
        texture.magFilter = THREE.LinearFilter
        texture.generateMipmaps = false
        texture.needsUpdate = true

        return texture
    }

    createExplorerRouteTexture(_label, _route, _accentColor, _index) {
        const canvas = document.createElement('canvas')
        canvas.width = 900
        canvas.height = 560
        const context = canvas.getContext('2d')

        if (!context) {
            return new THREE.CanvasTexture(canvas)
        }

        context.clearRect(0, 0, canvas.width, canvas.height)

        this.drawRoundedRect(context, 28, 28, canvas.width - 56, canvas.height - 56, 44)
        context.fillStyle = '#183422'
        context.fill()

        context.lineWidth = 10
        context.strokeStyle = 'rgba(255,255,255,0.14)'
        context.stroke()

        this.drawRoundedRect(context, 56, 56, canvas.width - 112, 82, 24)
        context.fillStyle = _accentColor
        context.fill()

        context.fillStyle = '#0e2215'
        context.font = '700 54px Arial'
        context.textAlign = 'left'
        context.textBaseline = 'middle'
        context.fillText(`${String(_index).padStart(2, '0')}`, 94, 98)

        const words = (_label || '').split(/\s+/)
        const lines = []
        let currentLine = ''
        const maxWidth = canvas.width - 120

        context.font = '700 82px Arial'
        for (const word of words) {
            const testLine = currentLine ? `${currentLine} ${word}` : word
            if (context.measureText(testLine).width <= maxWidth || !currentLine) {
                currentLine = testLine
            }
            else {
                lines.push(currentLine)
                currentLine = word
            }
        }

        if (currentLine) {
            lines.push(currentLine)
        }

        context.fillStyle = '#f5f2e8'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        context.font = '700 78px Arial'

        const lineHeight = 86
        const startY = lines.length > 1 ? 236 : 258
        lines.forEach((line, lineIndex) => {
            context.fillText(line.toUpperCase(), canvas.width * 0.5, startY + lineIndex * lineHeight)
        })

        context.fillStyle = 'rgba(255,255,255,0.58)'
        context.font = '700 42px Arial'
        context.fillText(_route, canvas.width * 0.5, canvas.height - 96)

        const texture = new THREE.CanvasTexture(canvas)
        texture.colorSpace = THREE.SRGBColorSpace
        texture.minFilter = THREE.LinearFilter
        texture.magFilter = THREE.LinearFilter
        texture.generateMipmaps = false
        texture.needsUpdate = true

        return texture
    }

    drawRoundedRect(_context, _x, _y, _width, _height, _radius) {
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

    setTiles() {
        this.tiles.add({
            start: new THREE.Vector2(this.x - 1.2, this.y + 13),
            delta: new THREE.Vector2(0, - 20)
        })
    }
}
