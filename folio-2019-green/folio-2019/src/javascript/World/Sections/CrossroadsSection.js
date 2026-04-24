import * as THREE from 'three'

export default class CrossroadsSection
{
    constructor(_options)
    {
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
        this.hideLegacyProjectsSign()
        this.removeOrangeStatues()
        this.setAgricultureModels()
        this.setTimelineSign()
        this.setTiles()
    }

    setStatic()
    {
        this.static = this.objects.add({
            base: this.resources.items.crossroadsStaticBase.scene,
            collision: this.resources.items.crossroadsStaticCollision.scene,
            floorShadowTexture: this.resources.items.crossroadsStaticFloorShadowTexture,
            offset: new THREE.Vector3(this.x, this.y, 0),
            mass: 0
        })
    }

    hideLegacyProjectsSign()
    {
        if(!this.static || !this.static.container)
        {
            return
        }

        const box = new THREE.Box3()
        const hideZones = [
            new THREE.Box3(
                new THREE.Vector3(this.x + 6.0, this.y + 1.2, 0.2),
                new THREE.Vector3(this.x + 15.2, this.y + 9.8, 4.8)
            ),
            new THREE.Box3(
                new THREE.Vector3(this.x + 4.6, this.y - 0.4, 0.2),
                new THREE.Vector3(this.x + 16.4, this.y + 2.4, 4.8)
            )
        ]

        this.static.container.updateMatrixWorld(true)

        this.static.container.traverse((_child) =>
        {
            if(!(_child instanceof THREE.Mesh))
            {
                return
            }

            box.setFromObject(_child)
            if(box.isEmpty())
            {
                return
            }

            const intersectsLegacySign = hideZones.some((_zone) => _zone.intersectsBox(box))

            if(!intersectsLegacySign)
            {
                return
            }

            const size = box.getSize(new THREE.Vector3())
            const center = box.getCenter(new THREE.Vector3())

            // Keep large ground/platform meshes visible; hide only sign-like pieces.
            const isLargeStaticPiece = size.x > 5 || size.y > 5
            const isTooLow = center.z < 0.55

            if(!isLargeStaticPiece && !isTooLow)
            {
                _child.visible = false
            }
        })
    }

    removeOrangeStatues()
    {
        if(!this.static || !this.static.container)
        {
            return
        }

        // Refresh matrices so getWorldPosition() is accurate
        this.static.container.updateMatrix()
        this.static.container.traverse((child) => { child.updateMatrix() })
        this.static.container.updateMatrixWorld(true)

        this.orangeStatuePositions = []
        const toRemove = []

        this.static.container.traverse((_child) =>
        {
            if(!(_child instanceof THREE.Mesh))
            {
                return
            }
            if(/^shadeOrange/i.test(_child.name))
            {
                const wp = new THREE.Vector3()
                _child.getWorldPosition(wp)
                this.orangeStatuePositions.push(wp)
                toRemove.push(_child)
            }
        })

        for(const child of toRemove)
        {
            if(child.parent)
            {
                child.parent.remove(child)
            }
        }
    }

    setAgricultureModels()
    {
        this.agricultureModels = {}
        this.agricultureModels.container = new THREE.Object3D()
        this.agricultureModels.animatedRotors = []
        this.container.add(this.agricultureModels.container)

        const platforms = [
            { x: this.x + 0,  y: this.y + 0,  z: 2.06, scale: 4.8, rotationZ: Math.PI * 0.08,  build: () => this.createSunflower()   },
            { x: this.x - 9,  y: this.y + 9,  z: 2.04, scale: 5.0, rotationZ: Math.PI * 0.12,  build: () => this.createWindTurbine() },
            { x: this.x - 9,  y: this.y - 9,  z: 2.00, scale: 5.4, rotationZ: -Math.PI * 0.06, build: () => this.createGrainSilo()   },
            { x: this.x + 9,  y: this.y - 9,  z: 1.98, scale: 4.9, rotationZ: -Math.PI * 0.14, build: () => this.createBarn()        },
            { x: this.x + 9,  y: this.y + 9,  z: 2.01, scale: 4.5, rotationZ: Math.PI * 0.20,  build: () => this.createTractor()     },
        ]

        for(const { x, y, z, scale, rotationZ, build } of platforms)
        {
            const model = build()
            model.position.set(x, y, z)
            model.rotation.z = rotationZ
            model.scale.setScalar(scale)

            if(model.userData && model.userData.rotor)
            {
                this.agricultureModels.animatedRotors.push(model.userData.rotor)
            }

            this.agricultureModels.container.add(model)
        }

        if(this.agricultureModels.animatedRotors.length > 0)
        {
            this.time.on('tick', () =>
            {
                const spinStep = this.time.delta * 0.0022
                for(const rotor of this.agricultureModels.animatedRotors)
                {
                    rotor.rotation.y += spinStep
                }
            })
        }
    }

    createSunflower()
    {
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
            { ox:  0.18, oz: 0.26, rz:  Math.PI * 0.38 },
            { ox: -0.18, oz: 0.26, rz: -Math.PI * 0.38 },
            { ox:  0.15, oz: 0.50, rz:  Math.PI * 0.32 },
            { ox: -0.15, oz: 0.50, rz: -Math.PI * 0.32 },
        ]
        for(const ld of leafData)
        {
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
        for(let i = 0; i < 18; i++)
        {
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
        for(let i = 0; i < 13; i++)
        {
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
        for(let ring = 0; ring < 4; ring++)
        {
            const r      = ring * 0.044
            const count  = ring === 0 ? 1 : Math.round(ring * 6.3)
            const sMat   = ring % 2 === 0 ? s1 : s2
            for(let s = 0; s < count; s++)
            {
                const ang  = (s / count) * Math.PI * 2
                const bump = new THREE.Mesh(new THREE.SphereGeometry(0.016, 6, 4), sMat)
                bump.position.set(Math.cos(ang) * r, Math.sin(ang) * r, 0.92)
                group.add(bump)
            }
        }

        return group
    }

    createWindTurbine()
    {
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
        for(let i = 0; i < 3; i++)
        {
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

    createGrainSilo()
    {
        const group = new THREE.Object3D()

        const siloMatA = new THREE.MeshBasicMaterial({ color: 0xddd4b0 })
        const siloMatB = new THREE.MeshBasicMaterial({ color: 0xcec598 })
        const roofMat  = new THREE.MeshBasicMaterial({ color: 0x8a6040 })
        const ribMat   = new THREE.MeshBasicMaterial({ color: 0xb8a870 })
        const baseMat  = new THREE.MeshBasicMaterial({ color: 0x989080 })

        // Two silo cylinders side by side
        for(const { ox, mat } of [{ ox: -0.2, mat: siloMatA }, { ox: 0.2, mat: siloMatB }])
        {
            const body = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.19, 0.80, 16), mat)
            body.rotation.x = Math.PI * 0.5
            body.position.set(ox, 0, 0.40)
            group.add(body)

            const roof = new THREE.Mesh(new THREE.ConeGeometry(0.205, 0.26, 16), roofMat)
            roof.rotation.x = Math.PI * 0.5
            roof.position.set(ox, 0, 0.93)
            group.add(roof)

            for(let r = 0; r < 5; r++)
            {
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
        railR.position.set(0.407,  0.035, 0.40)
        group.add(railR)
        for(let r = 0; r < 7; r++)
        {
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

    createBarn()
    {
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
        for(const cx of [-0.39, 0.39])
        {
            for(const cy of [-0.24, 0.24])
            {
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
        roofR.rotation.y =  0.58
        roofR.position.set( 0.19, 0, 0.635)
        group.add(roofR)
        const ridge = new THREE.Mesh(new THREE.BoxGeometry(0.058, 0.52, 0.058), roofMat)
        ridge.position.z = 0.835
        group.add(ridge)

        // Sliding door with X-brace
        const doorMat  = new THREE.MeshBasicMaterial({ color: 0x6a4020 })
        const doorFace = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.022, 0.30), doorMat)
        doorFace.position.set(0, -0.242, 0.155)
        group.add(doorFace)
        const braceA = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.022, 0.37), doorMat)
        braceA.rotation.x =  0.84
        braceA.position.set(0, -0.242, 0.155)
        group.add(braceA)
        const braceB = new THREE.Mesh(new THREE.BoxGeometry(0.013, 0.022, 0.37), doorMat)
        braceB.rotation.x = -0.84
        braceB.position.set(0, -0.242, 0.155)
        group.add(braceB)

        // Hayloft window + frame
        const glassMat = new THREE.MeshBasicMaterial({ color: 0x50a0c8 })
        const loftWin  = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.022, 0.10), glassMat)
        loftWin.position.set(0, -0.242, 0.43)
        group.add(loftWin)
        const loftFrame = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.025, 0.12), trimMat)
        loftFrame.position.set(0, -0.244, 0.43)
        group.add(loftFrame)

        // Side windows
        for(const wz of [0.26, 0.40])
        {
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

    createTractor()
    {
        const group = new THREE.Object3D()

        // Bright, saturated green palette
        const frameMat  = new THREE.MeshBasicMaterial({ color: 0x2e7e28 })
        const bodyMat   = new THREE.MeshBasicMaterial({ color: 0x38a030 })
        const darkMat   = new THREE.MeshBasicMaterial({ color: 0x245c20 })
        const cabMat    = new THREE.MeshBasicMaterial({ color: 0x40b838 })
        const glassMat  = new THREE.MeshBasicMaterial({ color: 0x60c0dc })
        const tireMat   = new THREE.MeshBasicMaterial({ color: 0x303030 })
        const silverMat = new THREE.MeshBasicMaterial({ color: 0xd0d0c0 })
        const greyMat   = new THREE.MeshBasicMaterial({ color: 0x909090 })
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
        for(let g = 0; g < 4; g++)
        {
            const bar = new THREE.Mesh(new THREE.BoxGeometry(0.030, 0.19, 0.013), silverMat)
            bar.position.set(0.385, 0, 0.335 + g * 0.040)
            group.add(bar)
        }

        // Headlights — bright yellow
        for(const hy of [-0.08, 0.08])
        {
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
        for(const wy of [-0.155, 0.155])
        {
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
        for(const ry of [0.23, -0.23])
        {
            const tire = new THREE.Mesh(new THREE.TorusGeometry(0.215, 0.085, 10, 20), tireMat)
            tire.rotation.x = Math.PI * 0.5
            tire.position.set(-0.18, ry, 0.215)
            group.add(tire)

            for(let t = 0; t < 8; t++)
            {
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
        for(const fy of [0.148, -0.148])
        {
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
        for(const hy of [0.12, -0.12])
        {
            const arm = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.017, 0.017), hitchMat)
            arm.position.set(-0.45, hy, 0.18)
            group.add(arm)
        }

        return group
    }

    setTiles()
    {
        // To intro
        this.tiles.add({
            start: new THREE.Vector2(this.x, - 10),
            delta: new THREE.Vector2(0, this.y + 14)
        })

        // To projects
        this.tiles.add({
            start: new THREE.Vector2(this.x + 12.5, this.y),
            delta: new THREE.Vector2(7.5, 0)
        })

        // To projects
        this.tiles.add({
            start: new THREE.Vector2(this.x - 13, this.y),
            delta: new THREE.Vector2(- 6, 0)
        })
    }

    setTimelineSign()
    {
        const signTexture = this.createTimelineSignTexture()
        const signMaterial = new THREE.MeshBasicMaterial({
            map: signTexture,
            transparent: true,
            depthWrite: false,
            side: THREE.DoubleSide
        })

        const sign = new THREE.Mesh(new THREE.PlaneGeometry(3.1, 0.95), signMaterial)
        sign.rotation.x = Math.PI * 0.5
        sign.position.set(this.x + 12.4, this.y + 1.1, 2.35)
        sign.matrixAutoUpdate = false
        sign.updateMatrix()

        this.container.add(sign)
    }

    createTimelineSignTexture()
    {
        const canvas = document.createElement('canvas')
        canvas.width = 1024
        canvas.height = 320
        const context = canvas.getContext('2d')

        if(!context)
        {
            return this.resources.items.areaOpenTexture
        }

        context.clearRect(0, 0, canvas.width, canvas.height)

        context.fillStyle = 'rgba(242, 241, 232, 0.98)'
        context.strokeStyle = 'rgba(127, 93, 58, 0.95)'
        context.lineWidth = 14
        context.beginPath()
        context.moveTo(24, 40)
        context.lineTo(canvas.width - 220, 40)
        context.lineTo(canvas.width - 30, canvas.height * 0.5)
        context.lineTo(canvas.width - 220, canvas.height - 40)
        context.lineTo(24, canvas.height - 40)
        context.closePath()
        context.fill()
        context.stroke()

        context.fillStyle = '#6c4a2f'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        context.font = '700 118px Arial'
        context.fillText('TIMELINE', canvas.width * 0.43, canvas.height * 0.53)

        const texture = new THREE.CanvasTexture(canvas)
        texture.colorSpace = THREE.SRGBColorSpace
        texture.needsUpdate = true

        return texture
    }
}
