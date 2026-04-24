import * as THREE from 'three'

export default class IntroSection
{
    constructor(_options)
    {
        // Options
        this.config = _options.config
        this.time = _options.time
        this.resources = _options.resources
        this.objects = _options.objects
        this.areas = _options.areas
        this.walls = _options.walls
        this.tiles = _options.tiles
        this.debug = _options.debug
        this.x = _options.x
        this.y = _options.y

        // Set up
        this.container = new THREE.Object3D()
        this.container.matrixAutoUpdate = false
        this.container.updateMatrix()

        this.setStatic()
        this.setInstructions()
        this.setOtherInstructions()
        this.setTitles()
        this.setTiles()
        this.setDikes()
    }

    setStatic()
    {
        // Strip unwanted meshes from the GLTF BEFORE objects.add() clones them
        const toRemove = []
        this.resources.items.introStaticBase.scene.traverse((child) =>
        {
            if(child.isMesh && /^shade(Green|Orange)/i.test(child.name))
            {
                toRemove.push(child)
            }
        })
        toRemove.forEach((m) => { if(m.parent) m.parent.remove(m) })

        this.objects.add({
            base: this.resources.items.introStaticBase.scene,
            collision: this.resources.items.introStaticCollision.scene,
            floorShadowTexture: this.resources.items.introStaticFloorShadowTexture,
            offset: new THREE.Vector3(0, 0, 0),
            mass: 0
        })
    }

    setInstructions()
    {
        this.instructions = {}

        /**
         * Arrows
         */
        this.instructions.arrows = {}

        // Label
        this.instructions.arrows.label = {}

        this.instructions.arrows.label.texture = this.config.touch ? this.resources.items.introInstructionsControlsTexture : this.resources.items.introInstructionsArrowsTexture
        this.instructions.arrows.label.texture.magFilter = THREE.NearestFilter
        this.instructions.arrows.label.texture.minFilter = THREE.LinearFilter

        this.instructions.arrows.label.material = new THREE.MeshBasicMaterial({ transparent: true, alphaMap: this.instructions.arrows.label.texture, color: 0xffffff, depthWrite: false, opacity: 0 })

        this.instructions.arrows.label.geometry = this.resources.items.introInstructionsLabels.scene.children.find((_mesh) => _mesh.name === 'arrows').geometry

        this.instructions.arrows.label.mesh = new THREE.Mesh(this.instructions.arrows.label.geometry, this.instructions.arrows.label.material)
        this.container.add(this.instructions.arrows.label.mesh)

        if(!this.config.touch)
        {
            // Keys
            this.instructions.arrows.up = this.objects.add({
                base: this.resources.items.introArrowKeyBase.scene,
                collision: this.resources.items.introArrowKeyCollision.scene,
                offset: new THREE.Vector3(0, 0, 0),
                rotation: new THREE.Euler(0, 0, 0),
                duplicated: true,
                shadow: { sizeX: 1, sizeY: 1, offsetZ: - 0.2, alpha: 0.5 },
                mass: 1.5,
                soundName: 'brick'
            })
            this.instructions.arrows.down = this.objects.add({
                base: this.resources.items.introArrowKeyBase.scene,
                collision: this.resources.items.introArrowKeyCollision.scene,
                offset: new THREE.Vector3(0, - 0.8, 0),
                rotation: new THREE.Euler(0, 0, Math.PI),
                duplicated: true,
                shadow: { sizeX: 1, sizeY: 1, offsetZ: - 0.2, alpha: 0.5 },
                mass: 1.5,
                soundName: 'brick'
            })
            this.instructions.arrows.left = this.objects.add({
                base: this.resources.items.introArrowKeyBase.scene,
                collision: this.resources.items.introArrowKeyCollision.scene,
                offset: new THREE.Vector3(- 0.8, - 0.8, 0),
                rotation: new THREE.Euler(0, 0, Math.PI * 0.5),
                duplicated: true,
                shadow: { sizeX: 1, sizeY: 1, offsetZ: - 0.2, alpha: 0.5 },
                mass: 1.5,
                soundName: 'brick'
            })
            this.instructions.arrows.right = this.objects.add({
                base: this.resources.items.introArrowKeyBase.scene,
                collision: this.resources.items.introArrowKeyCollision.scene,
                offset: new THREE.Vector3(0.8, - 0.8, 0),
                rotation: new THREE.Euler(0, 0, - Math.PI * 0.5),
                duplicated: true,
                shadow: { sizeX: 1, sizeY: 1, offsetZ: - 0.2, alpha: 0.5 },
                mass: 1.5,
                soundName: 'brick'
            })
        }
    }

    setOtherInstructions()
    {
        if(this.config.touch)
        {
            return
        }

        this.otherInstructions = {}
        this.otherInstructions.x = 16
        this.otherInstructions.y = - 2

        // Container
        this.otherInstructions.container = new THREE.Object3D()
        this.otherInstructions.container.position.x = this.otherInstructions.x
        this.otherInstructions.container.position.y = this.otherInstructions.y
        this.otherInstructions.container.matrixAutoUpdate = false
        this.otherInstructions.container.updateMatrix()
        this.container.add(this.otherInstructions.container)

        // Label
        this.otherInstructions.label = {}

        this.otherInstructions.label.geometry = new THREE.PlaneGeometry(6, 6, 1, 1)

        this.otherInstructions.label.texture = this.resources.items.introInstructionsOtherTexture
        this.otherInstructions.label.texture.magFilter = THREE.NearestFilter
        this.otherInstructions.label.texture.minFilter = THREE.LinearFilter

        this.otherInstructions.label.material = new THREE.MeshBasicMaterial({ transparent: true, alphaMap: this.otherInstructions.label.texture, color: 0xffffff, depthWrite: false, opacity: 0 })

        this.otherInstructions.label.mesh = new THREE.Mesh(this.otherInstructions.label.geometry, this.otherInstructions.label.material)
        this.otherInstructions.label.mesh.matrixAutoUpdate = false
        this.otherInstructions.container.add(this.otherInstructions.label.mesh)

        // Horn
        this.otherInstructions.horn = this.objects.add({
            base: this.resources.items.hornBase.scene,
            collision: this.resources.items.hornCollision.scene,
            offset: new THREE.Vector3(this.otherInstructions.x + 1.25, this.otherInstructions.y - 2.75, 0.2),
            rotation: new THREE.Euler(0, 0, 0.5),
            duplicated: true,
            shadow: { sizeX: 1.65, sizeY: 0.75, offsetZ: - 0.1, alpha: 0.4 },
            mass: 1.5,
            soundName: 'horn',
            sleep: false
        })
    }

    setTitles()
    {
        // ── Panneau routier "GREEN ROOTS" ─────────────────────────────────────
        // Monde Z-up. Caméra à (1.135, -1.45, 1.15) regarde vers l'origine.
        // Le panneau est vertical dans le plan XZ, face vers Y- (face caméra).
        // MeshBasicMaterial obligatoire — aucune lumière dans la scène.

        const signGroup = new THREE.Group()
        signGroup.matrixAutoUpdate = false

        const W   = 8.0   // largeur panneau
        const H   = 2.2   // hauteur panneau
        const D   = 0.2   // épaisseur
        const brd = 0.14  // épaisseur bordure

        const greenMat = new THREE.MeshBasicMaterial({ color: 0x0d47a1 })
        const whiteMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
        const grayMat  = new THREE.MeshBasicMaterial({ color: 0x888888 })

        // ── Panneau fond vert — dans le plan XZ, debout en Z ─────────────────
        // BoxGeometry(W, H, D) : W=X, H=Y, D=Z
        // On veut : large en X, haut en Z, fin en Y
        // → BoxGeometry(W, D, H), pas de rotation supplémentaire
        const board = new THREE.Mesh(new THREE.BoxGeometry(W, D, H), greenMat)
        board.position.set(0, 0, H / 2 + 2.0)
        signGroup.add(board)

        // ── Bordures blanches ────────────────────────────────────────────────
        // Haut (Z+)
        const bT = new THREE.Mesh(new THREE.BoxGeometry(W + brd*2, D+0.02, brd), whiteMat)
        bT.position.set(0, 0, H + brd/2 + 2.0)
        signGroup.add(bT)
        // Bas (Z-)
        const bBt = new THREE.Mesh(new THREE.BoxGeometry(W + brd*2, D+0.02, brd), whiteMat)
        bBt.position.set(0, 0, brd/2 + 2.0)
        signGroup.add(bBt)
        // Gauche (X-)
        const bLf = new THREE.Mesh(new THREE.BoxGeometry(brd, D+0.02, H + brd*2), whiteMat)
        bLf.position.set(-(W/2 + brd/2), 0, H/2 + 2.0)
        signGroup.add(bLf)
        // Droite (X+)
        const bRt = new THREE.Mesh(new THREE.BoxGeometry(brd, D+0.02, H + brd*2), whiteMat)
        bRt.position.set(W/2 + brd/2, 0, H/2 + 2.0)
        signGroup.add(bRt)

        // ── Texte "GREEN ROOTS" ──────────────────────────────────────────────
        const canvas = document.createElement('canvas')
        canvas.width  = 1024
        canvas.height = 256
        this.signTitleCanvas = canvas
        this.signTitleContext = canvas.getContext('2d')
        if(!this.signTitleContext)
        {
            return
        }
        this.signTitleTexture = new THREE.CanvasTexture(canvas)
        this.signTitleTexture.colorSpace = THREE.SRGBColorSpace
        this.signTitleTexture.minFilter = THREE.LinearFilter
        this.signTitleTexture.magFilter = THREE.LinearFilter
        this.signTitleTexture.generateMipmaps = false

        this.setSignTitle(this.config.signTitle || 'GREEN ROOTS')
        const tex = this.signTitleTexture

        this.signTitleSpeed = this.config.signTitleSpeed || 140
        this.signTitleScrollOffset = 0

        this.time.on('tick', () =>
        {
            this.signTitleScrollOffset += this.signTitleSpeed * (this.time.delta / 1000)
            this.setSignTitle(this.signTitle)
        })

        // PlaneGeometry dans XZ (face vers Y-)
        // PlaneGeometry par défaut est dans XY — on tourne de 90° autour de X
        const textMatFront = new THREE.MeshBasicMaterial({ map: tex, transparent: true, depthWrite: false, side: THREE.FrontSide })
        const textMatBack  = new THREE.MeshBasicMaterial({ map: tex, transparent: true, depthWrite: false, side: THREE.FrontSide })

        // Face avant (Y-)
        const planeFront = new THREE.Mesh(new THREE.PlaneGeometry(W * 0.88, H * 0.72), textMatFront)
        planeFront.rotation.x = Math.PI / 2  // couche le plan dans XZ, face vers Y-
        planeFront.position.set(0, -(D/2 + 0.01), H/2 + 2.0)
        signGroup.add(planeFront)

        // Face arrière (Y+)
        const planeBack = new THREE.Mesh(new THREE.PlaneGeometry(W * 0.88, H * 0.72), textMatBack)
        planeBack.rotation.x = -Math.PI / 2
        planeBack.rotation.y = Math.PI
        planeBack.position.set(0, D/2 + 0.01, H/2 + 2.0)
        signGroup.add(planeBack)

        // ── Poteaux ──────────────────────────────────────────────────────────
        const poleH = 2.15
        const poleGeo = new THREE.CylinderGeometry(0.09, 0.09, poleH, 8)

        const pL = new THREE.Mesh(poleGeo, grayMat)
        pL.rotation.x = Math.PI / 2  // debout selon Z
        pL.position.set(-W * 0.28, 0, poleH/2)
        signGroup.add(pL)

        const pR = new THREE.Mesh(poleGeo, grayMat)
        pR.rotation.x = Math.PI / 2
        pR.position.set(W * 0.28, 0, poleH/2)
        signGroup.add(pR)

        // ── Position dans la scène ───────────────────────────────────────────
        signGroup.position.set(this.x + 1.5, this.y + 3, 0)
        signGroup.updateMatrix()
        this.container.add(signGroup)
    }

    setSignTitle(_title)
    {
        this.signTitle = (_title || '').trim() || 'GREEN ROOTS'

        if(!this.signTitleCanvas || !this.signTitleContext || !this.signTitleTexture)
        {
            return
        }

        const canvas = this.signTitleCanvas
        const ctx = this.signTitleContext
        const title = this.signTitle
        const fontFamily = '"Arial Black", Arial, sans-serif'
        const maxWidth = canvas.width * 0.9

        ctx.clearRect(0, 0, canvas.width, canvas.height)
        ctx.fillStyle = '#ffffff'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'

        let fontSize = 138
        while(fontSize > 40)
        {
            ctx.font = `bold ${fontSize}px ${fontFamily}`

            if(ctx.measureText(title).width <= maxWidth)
            {
                break
            }

            fontSize -= 2
        }

        const textWidth = ctx.measureText(title).width
        const gap = canvas.width * 0.2
        const cycleWidth = textWidth + gap
        const offset = cycleWidth > 0 ? this.signTitleScrollOffset % cycleWidth : 0
        const y = canvas.height * 0.52

        // Titre qui defile en boucle sur toute la largeur du panneau.
        for(let x = canvas.width * 0.5 - offset; x < canvas.width + cycleWidth; x += cycleWidth)
        {
            ctx.fillText(title, x, y)
        }

        this.signTitleTexture.needsUpdate = true
    }

    setTiles()
    {
        this.tiles.add({
            start: new THREE.Vector2(0, - 4.5),
            delta: new THREE.Vector2(0, - 4.5)
        })
    }

    setDikes()
    {
        this.dikes = {}
        this.dikes.brickOptions = {
            base: this.resources.items.brickBase.scene,
            collision: this.resources.items.brickCollision.scene,
            offset: new THREE.Vector3(0, 0, 0.1),
            rotation: new THREE.Euler(0, 0, 0),
            duplicated: true,
            shadow: { sizeX: 1.2, sizeY: 1.8, offsetZ: - 0.15, alpha: 0.35 },
            mass: 0.5,
            soundName: 'brick'
        }

        // this.walls.add({
        //     object:
        //     {
        //         ...this.dikes.brickOptions,
        //         rotation: new THREE.Euler(0, 0, Math.PI * 0.5)
        //     },
        //     shape:
        //     {
        //         type: 'brick',
        //         equilibrateLastLine: true,
        //         widthCount: 3,
        //         heightCount: 2,
        //         position: new THREE.Vector3(this.x + 0, this.y - 4, 0),
        //         offsetWidth: new THREE.Vector3(1.05, 0, 0),
        //         offsetHeight: new THREE.Vector3(0, 0, 0.45),
        //         randomOffset: new THREE.Vector3(0, 0, 0),
        //         randomRotation: new THREE.Vector3(0, 0, 0.2)
        //     }
        // })

        this.walls.add({
            object: this.dikes.brickOptions,
            shape:
            {
                type: 'brick',
                equilibrateLastLine: true,
                widthCount: 5,
                heightCount: 2,
                position: new THREE.Vector3(this.x - 12, this.y - 13, 0),
                offsetWidth: new THREE.Vector3(0, 1.05, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        this.walls.add({
            object:
            {
                ...this.dikes.brickOptions,
                rotation: new THREE.Euler(0, 0, Math.PI * 0.5)
            },
            shape:
            {
                type: 'brick',
                equilibrateLastLine: true,
                widthCount: 3,
                heightCount: 2,
                position: new THREE.Vector3(this.x + 8, this.y + 6, 0),
                offsetWidth: new THREE.Vector3(1.05, 0, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        this.walls.add({
            object: this.dikes.brickOptions,
            shape:
            {
                type: 'brick',
                equilibrateLastLine: false,
                widthCount: 3,
                heightCount: 2,
                position: new THREE.Vector3(this.x + 9.9, this.y + 4.7, 0),
                offsetWidth: new THREE.Vector3(0, - 1.05, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        this.walls.add({
            object:
            {
                ...this.dikes.brickOptions,
                rotation: new THREE.Euler(0, 0, Math.PI * 0.5)
            },
            shape:
            {
                type: 'brick',
                equilibrateLastLine: true,
                widthCount: 3,
                heightCount: 2,
                position: new THREE.Vector3(this.x - 14, this.y + 2, 0),
                offsetWidth: new THREE.Vector3(1.05, 0, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        this.walls.add({
            object: this.dikes.brickOptions,
            shape:
            {
                type: 'brick',
                equilibrateLastLine: false,
                widthCount: 3,
                heightCount: 2,
                position: new THREE.Vector3(this.x - 14.8, this.y + 0.7, 0),
                offsetWidth: new THREE.Vector3(0, - 1.05, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        this.walls.add({
            object: this.dikes.brickOptions,
            shape:
            {
                type: 'brick',
                equilibrateLastLine: true,
                widthCount: 3,
                heightCount: 2,
                position: new THREE.Vector3(this.x - 14.8, this.y - 3.5, 0),
                offsetWidth: new THREE.Vector3(0, - 1.05, 0),
                offsetHeight: new THREE.Vector3(0, 0, 0.45),
                randomOffset: new THREE.Vector3(0, 0, 0),
                randomRotation: new THREE.Vector3(0, 0, 0.2)
            }
        })

        if(!this.config.touch)
        {
            this.walls.add({
                object:
                {
                    ...this.dikes.brickOptions,
                    rotation: new THREE.Euler(0, 0, Math.PI * 0.5)
                },
                shape:
                {
                    type: 'brick',
                    equilibrateLastLine: true,
                    widthCount: 2,
                    heightCount: 2,
                    position: new THREE.Vector3(this.x + 18.5, this.y + 3, 0),
                    offsetWidth: new THREE.Vector3(1.05, 0, 0),
                    offsetHeight: new THREE.Vector3(0, 0, 0.45),
                    randomOffset: new THREE.Vector3(0, 0, 0),
                    randomRotation: new THREE.Vector3(0, 0, 0.2)
                }
            })

            this.walls.add({
                object: this.dikes.brickOptions,
                shape:
                {
                    type: 'brick',
                    equilibrateLastLine: false,
                    widthCount: 2,
                    heightCount: 2,
                    position: new THREE.Vector3(this.x + 19.9, this.y + 2.2, 0),
                    offsetWidth: new THREE.Vector3(0, - 1.05, 0),
                    offsetHeight: new THREE.Vector3(0, 0, 0.45),
                    randomOffset: new THREE.Vector3(0, 0, 0),
                    randomRotation: new THREE.Vector3(0, 0, 0.2)
                }
            })
        }
    }

    // Agriculture decorations placed on the white keyboard platform (shadeWhite_013/014
    // sit at GLTF local ~(2.5, 4, 0); top surface ≈ z 0.5).
    setAgricultureDecorations()
    {
        const group = new THREE.Object3D()

        // --- Wheat rows ---
        const stemMat  = new THREE.MeshBasicMaterial({ color: 0xd4a017 })
        const grainMat = new THREE.MeshBasicMaterial({ color: 0xc8860a })
        const leafMat2 = new THREE.MeshBasicMaterial({ color: 0x6aaa30 })
        for(let row = 0; row < 3; row++)
        {
            for(let col = 0; col < 5; col++)
            {
                const stalk = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.06, 0.7, 6), stemMat)
                stalk.rotation.x = Math.PI * 0.5
                stalk.position.set(-1.8 + col * 0.55, -0.4 + row * 0.55, 0.35)
                group.add(stalk)

                const head = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.09, 0.28, 6), grainMat)
                head.rotation.x = Math.PI * 0.5
                head.position.set(-1.8 + col * 0.55, -0.4 + row * 0.55, 0.87)
                group.add(head)

                const leaf = new THREE.Mesh(new THREE.BoxGeometry(0.28, 0.04, 0.06), leafMat2)
                leaf.position.set(-1.8 + col * 0.55, -0.4 + row * 0.55, 0.52)
                leaf.rotation.z = Math.PI * 0.25 * (col % 2 === 0 ? 1 : -1)
                group.add(leaf)
            }
        }

        // --- Haystack ---
        const hayMat = new THREE.MeshBasicMaterial({ color: 0xe8c04a })
        const hay = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.6, 0.8, 10), hayMat)
        hay.rotation.x = Math.PI * 0.5
        hay.position.set(1.8, 0.2, 0.4)
        group.add(hay)
        const hayTop = new THREE.Mesh(new THREE.SphereGeometry(0.55, 10, 6, 0, Math.PI * 2, 0, Math.PI * 0.5), hayMat)
        hayTop.rotation.x = Math.PI * 0.5
        hayTop.position.set(1.8, 0.2, 0.8)
        group.add(hayTop)

        // --- Scarecrow ---
        const woodMat  = new THREE.MeshBasicMaterial({ color: 0x8b5e3c })
        const shirtMat = new THREE.MeshBasicMaterial({ color: 0x5c8a3c })
        const hatMat   = new THREE.MeshBasicMaterial({ color: 0x3d2b1f })

        const post = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 1.4, 6), woodMat)
        post.rotation.x = Math.PI * 0.5
        post.position.set(-2.2, 1.5, 0.7)
        group.add(post)

        const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.9, 6), woodMat)
        arm.rotation.z = Math.PI * 0.5
        arm.position.set(-2.2, 1.5, 0.9)
        group.add(arm)

        const body = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.1, 0.4), shirtMat)
        body.position.set(-2.2, 1.5, 0.9)
        group.add(body)

        const head = new THREE.Mesh(new THREE.SphereGeometry(0.15, 8, 6), new THREE.MeshBasicMaterial({ color: 0xf5d08a }))
        head.position.set(-2.2, 1.5, 1.26)
        group.add(head)

        const hat = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.18, 0.22, 8), hatMat)
        hat.rotation.x = Math.PI * 0.5
        hat.position.set(-2.2, 1.5, 1.48)
        group.add(hat)

        // --- Water pump ---
        const metalMat = new THREE.MeshBasicMaterial({ color: 0x607070 })
        const base2 = new THREE.Mesh(new THREE.BoxGeometry(0.24, 0.24, 0.12), metalMat)
        base2.position.set(2.0, 1.8, 0.06)
        group.add(base2)
        const pipe = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 0.6, 8), metalMat)
        pipe.rotation.x = Math.PI * 0.5
        pipe.position.set(2.0, 1.8, 0.42)
        group.add(pipe)
        const spout = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.05, 0.28, 8), metalMat)
        spout.rotation.z = Math.PI * 0.5
        spout.position.set(2.0, 1.65, 0.72)
        group.add(spout)
        const handle = new THREE.Mesh(new THREE.TorusGeometry(0.1, 0.02, 6, 10, Math.PI), metalMat)
        handle.rotation.x = Math.PI * 0.5
        handle.position.set(2.0, 1.95, 0.72)
        group.add(handle)

        // Place the whole group on top of the keyboard platform
        group.position.set(this.x + 2.5, this.y + 4.0, 0.55)
        group.scale.setScalar(1.4)
        this.container.add(group)
    }
}
