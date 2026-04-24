import * as THREE from 'three'

import ProjectBoardMaterial from '../../Materials/ProjectBoard.js'
import gsap from 'gsap'

export default class Project
{
    static viewer = null
    static viewerText = null
    static viewerGallery = null
    static imageViewer = null
    static imageViewerImage = null
    static tvViewer = null
    static tvViewerImage = null
    static focusOwner = null

    static clearFocusOwner()
    {
        if(Project.focusOwner && Project.focusOwner.camera && Project.focusOwner.camera.focus.active)
        {
            Project.focusOwner.camera.clearFocus()
        }

        Project.focusOwner = null
    }

    constructor(_options)
    {
        // Options
        this.time = _options.time
        this.resources = _options.resources
        this.objects = _options.objects
        this.areas = _options.areas
        this.camera = _options.camera
        this.name = _options.name
        this.geometries = _options.geometries
        this.meshes = _options.meshes
        this.debug = _options.debug
        this.name = _options.name
        this.x = _options.x
        this.y = _options.y
        this.scale = _options.scale || 1
        this.carousel = !!_options.carousel
        this.carouselDelay = _options.carouselDelay || 2.8
        this.focusOnInteract = !!_options.focusOnInteract
        this.focusOnlyOnInteract = !!_options.focusOnlyOnInteract
        this.focusAngle = _options.focusAngle || 'projectsClose'
        this.focusTargetZ = typeof _options.focusTargetZ === 'number' ? _options.focusTargetZ : 1.5
        this.focusZoom = typeof _options.focusZoom === 'number' ? _options.focusZoom : 0
        this.openTvOverlay = _options.openTvOverlay !== false
        this.imageSources = _options.imageSources
        this.floorTexture = _options.floorTexture
        this.floorTextParagraphs = Array.isArray(_options.floorTextParagraphs) ? _options.floorTextParagraphs : []
        this.link = _options.link
        this.distinctions = _options.distinctions

        // Set up
        this.container = new THREE.Object3D()
        this.container.matrixAutoUpdate = false
        this.container.updateMatrix()

        this.setBoards()
        this.setFloor()
        this.setViewer()

        if(this.focusOnInteract)
        {
            window.addEventListener('keydown', (_event) =>
            {
                if(_event.key === 'Escape' && this.camera && this.camera.focus.active)
                {
                    this.camera.clearFocus()
                }
            })
        }
    }

    setViewer()
    {
        if(Project.viewer)
        {
            return
        }

        Project.viewer = document.createElement('section')
        Project.viewer.className = 'project-viewer'

        const panel = document.createElement('div')
        panel.className = 'project-viewer__panel'
        Project.viewer.appendChild(panel)

        const close = document.createElement('button')
        close.className = 'project-viewer__close'
        close.type = 'button'
        close.textContent = 'Close'
        panel.appendChild(close)

        Project.viewerText = document.createElement('div')
        Project.viewerText.className = 'project-viewer__text'
        panel.appendChild(Project.viewerText)

        Project.viewerGallery = document.createElement('div')
        Project.viewerGallery.className = 'project-viewer__gallery'
        panel.appendChild(Project.viewerGallery)

        const closeViewer = () =>
        {
            Project.viewer.classList.remove('is-visible')
            document.body.classList.remove('has-project-viewer')
            Project.clearFocusOwner()
        }

        close.addEventListener('click', closeViewer)
        Project.viewer.addEventListener('click', (_event) =>
        {
            if(_event.target === Project.viewer)
            {
                closeViewer()
            }
        })

        window.addEventListener('keydown', (_event) =>
        {
            if(_event.key === 'Escape' && Project.viewer.classList.contains('is-visible'))
            {
                closeViewer()
            }
        })

        document.body.appendChild(Project.viewer)

        Project.imageViewer = document.createElement('section')
        Project.imageViewer.className = 'project-image-viewer'

        Project.imageViewerImage = document.createElement('img')
        Project.imageViewerImage.className = 'project-image-viewer__image'
        Project.imageViewer.appendChild(Project.imageViewerImage)

        Project.imageViewer.addEventListener('click', () =>
        {
            Project.imageViewer.classList.remove('is-visible')
        })

        window.addEventListener('keydown', (_event) =>
        {
            if(_event.key === 'Escape' && Project.imageViewer.classList.contains('is-visible'))
            {
                Project.imageViewer.classList.remove('is-visible')
            }
        })

        document.body.appendChild(Project.imageViewer)

        Project.tvViewer = document.createElement('section')
        Project.tvViewer.className = 'project-tv-viewer'

        const tvFrame = document.createElement('div')
        tvFrame.className = 'project-tv-viewer__frame'
        Project.tvViewer.appendChild(tvFrame)

        Project.tvViewerImage = document.createElement('img')
        Project.tvViewerImage.className = 'project-tv-viewer__image'
        tvFrame.appendChild(Project.tvViewerImage)

        const closeTvViewer = () =>
        {
            Project.tvViewer.classList.remove('is-visible')
            Project.clearFocusOwner()
        }

        Project.tvViewer.addEventListener('click', (_event) =>
        {
            if(_event.target === Project.tvViewer)
            {
                closeTvViewer()
            }
        })

        window.addEventListener('keydown', (_event) =>
        {
            if(_event.key === 'Escape' && Project.tvViewer.classList.contains('is-visible'))
            {
                closeTvViewer()
            }
        })

        document.body.appendChild(Project.tvViewer)
    }

    openViewer()
    {
        if(this.focusOnInteract && this.camera && this.boards.items.length > 0)
        {
            const board = this.boards.items[0]
            Project.focusOwner = this
            this.camera.setFocus({
                target: { x: board.x, y: board.y, z: this.focusTargetZ },
                angle: this.focusAngle,
                zoom: this.focusZoom
            })

            if(this.focusOnlyOnInteract)
            {
                return
            }
        }

        if(this.carousel && this.imageSources.length > 0)
        {
            if(this.openTvOverlay)
            {
                this.openTvViewer()
            }
            return
        }

        Project.viewerText.innerHTML = ''

        for(const paragraph of this.floorTextParagraphs)
        {
            const $paragraph = document.createElement('p')
            $paragraph.textContent = paragraph
            Project.viewerText.appendChild($paragraph)
        }

        Project.viewerGallery.innerHTML = ''

        for(const imageSource of this.imageSources)
        {
            const image = document.createElement('img')
            image.className = 'project-viewer__image'
            image.src = imageSource
            image.alt = this.name
            image.loading = 'lazy'
            image.addEventListener('click', () =>
            {
                Project.imageViewerImage.src = imageSource
                Project.imageViewerImage.alt = this.name
                Project.imageViewer.classList.add('is-visible')
            })
            Project.viewerGallery.appendChild(image)
        }

        Project.viewer.classList.add('is-visible')
        document.body.classList.add('has-project-viewer')
    }

    openTvViewer()
    {
        const currentSource = this.imageSources[this.carouselState ? this.carouselState.index : 0]
        if(!currentSource)
        {
            return
        }

        Project.tvViewerImage.src = currentSource
        Project.tvViewerImage.alt = this.name
        Project.tvViewer.classList.add('is-visible')
    }

    setBoards()
    {
        // Set up
        this.boards = {}
        this.boards.items = []
        this.boards.xStart = - 5 * this.scale
        this.boards.xInter = 5 * this.scale
        this.boards.y = 5 * this.scale
        this.boards.color = '#8e7161'
        this.boards.threeColor = new THREE.Color(this.boards.color)

        if(this.debug)
        {
            this.debug.addColor(this.boards, 'color').name('boardColor').onChange(() =>
            {
                this.boards.threeColor.set(this.boards.color)
            })
        }

        const boardSources = this.carousel ? [this.imageSources[0]] : this.imageSources

        // Create each board
        let i = 0

        for(const _imageSource of boardSources)
        {
            // Set up
            const board = {}
            board.x = this.x + this.boards.xStart + i * this.boards.xInter
            board.y = this.y + this.boards.y

            // Create structure with collision
            this.objects.add({
                base: this.resources.items.projectsBoardStructure.scene,
                collision: this.resources.items.projectsBoardCollision.scene,
                floorShadowTexture: this.resources.items.projectsBoardStructureFloorShadowTexture,
                offset: new THREE.Vector3(board.x, board.y, 0),
                rotation: new THREE.Euler(0, 0, 0),
                duplicated: true,
                mass: 0
            })

            // Image load
            const image = new Image()
            image.addEventListener('load', () =>
            {
                board.texture = new THREE.Texture(image)
                // board.texture.magFilter = THREE.NearestFilter
                // board.texture.minFilter = THREE.LinearFilter
                board.texture.anisotropy = 4
                // board.texture.colorSpace = THREE.SRGBColorSpace
                board.texture.needsUpdate = true

                board.planeMesh.material.uniforms.uTexture.value = board.texture

                gsap.to(board.planeMesh.material.uniforms.uTextureAlpha, { value: 1, duration: 1, ease: 'power4.inOut' })
            })

            image.src = _imageSource

            // Plane
            board.planeMesh = this.meshes.boardPlane.clone()
            board.planeMesh.position.x = board.x
            board.planeMesh.position.y = board.y
            board.planeMesh.matrixAutoUpdate = false
            board.planeMesh.updateMatrix()
            board.planeMesh.material = new ProjectBoardMaterial()
            board.planeMesh.material.uniforms.uColor.value = this.boards.threeColor
            board.planeMesh.material.uniforms.uTextureAlpha.value = 0
            this.container.add(board.planeMesh)

            // Save
            this.boards.items.push(board)

            i++
        }

        if(this.carousel && this.boards.items.length === 1 && this.imageSources.length > 1)
        {
            this.setCarousel(this.boards.items[0])
        }
    }

    setCarousel(_board)
    {
        this.carouselState = {
            board: _board,
            textures: [],
            index: 0,
            isTransitioning: false,
            loader: new THREE.TextureLoader()
        }

        const applyTexture = (_texture, _animate = false) =>
        {
            _texture.anisotropy = 4
            _texture.magFilter = THREE.LinearFilter
            _texture.minFilter = THREE.LinearFilter
            _texture.needsUpdate = true
            _board.planeMesh.material.uniforms.uTexture.value = _texture

            if(_animate)
            {
                _board.planeMesh.material.uniforms.uTextureAlpha.value = 0
                gsap.to(_board.planeMesh.material.uniforms.uTextureAlpha, { value: 1, duration: 0.8, ease: 'power2.inOut' })
            }
            else
            {
                _board.planeMesh.material.uniforms.uTextureAlpha.value = 1
            }
        }

        const showNext = () =>
        {
            if(this.carouselState.textures.length < 2 || this.carouselState.isTransitioning)
            {
                return
            }

            this.carouselState.isTransitioning = true
            this.carouselState.index = (this.carouselState.index + 1) % this.carouselState.textures.length
            applyTexture(this.carouselState.textures[this.carouselState.index], true)
            gsap.delayedCall(0.85, () =>
            {
                this.carouselState.isTransitioning = false
            })
            gsap.delayedCall(this.carouselDelay, showNext)
        }

        for(const imageSource of this.imageSources)
        {
            this.carouselState.loader.load(imageSource, (_texture) =>
            {
                this.carouselState.textures.push(_texture)

                if(this.carouselState.textures.length === 1)
                {
                    applyTexture(_texture, false)
                }

                if(this.carouselState.textures.length === 2)
                {
                    gsap.delayedCall(this.carouselDelay, showNext)
                }
            })
        }
    }

    setFloor()
    {
        this.floor = {}

        this.floor.x = 0
        this.floor.y = - 2

        // Container
        this.floor.container = new THREE.Object3D()
        this.floor.container.position.x = this.x + this.floor.x * this.scale
        this.floor.container.position.y = this.y + this.floor.y * this.scale
        this.floor.container.matrixAutoUpdate = false
        this.floor.container.updateMatrix()
        this.container.add(this.floor.container)

        // Texture
        const hasCustomFloorText = this.floorTextParagraphs.length > 0
        this.floor.texture = hasCustomFloorText ? this.createFloorTextTexture(this.floorTextParagraphs) : this.floorTexture
        this.floor.texture.magFilter = hasCustomFloorText ? THREE.LinearFilter : THREE.NearestFilter
        this.floor.texture.minFilter = THREE.LinearFilter

        // Geometry
        this.floor.geometry = this.geometries.floor

        // Material
        this.floor.material =  new THREE.MeshBasicMaterial({ transparent: true, depthWrite: false, alphaMap: this.floor.texture })

        // Mesh
        this.floor.mesh = new THREE.Mesh(this.floor.geometry, this.floor.material)
        this.floor.mesh.matrixAutoUpdate = false
        this.floor.container.add(this.floor.mesh)

        // Distinctions
        if(this.distinctions)
        {
            for(const _distinction of this.distinctions)
            {
                let base = null
                let collision = null
                let shadowSizeX = null
                let shadowSizeY = null

                switch(_distinction.type)
                {
                    case 'awwwards':
                        base = this.resources.items.projectsDistinctionsAwwwardsBase.scene
                        collision = this.resources.items.projectsDistinctionsAwwwardsCollision.scene
                        shadowSizeX = 1.5
                        shadowSizeY = 1.5
                        break

                    case 'fwa':
                        base = this.resources.items.projectsDistinctionsFWABase.scene
                        collision = this.resources.items.projectsDistinctionsFWACollision.scene
                        shadowSizeX = 2
                        shadowSizeY = 1
                        break

                    case 'cssda':
                        base = this.resources.items.projectsDistinctionsCSSDABase.scene
                        collision = this.resources.items.projectsDistinctionsCSSDACollision.scene
                        shadowSizeX = 1.2
                        shadowSizeY = 1.2
                    break
                }

                this.objects.add({
                    base: base,
                    collision: collision,
                    offset: new THREE.Vector3(this.x + (this.floor.x + _distinction.x) * this.scale, this.y + (this.floor.y + _distinction.y) * this.scale, 0),
                    rotation: new THREE.Euler(0, 0, 0),
                    duplicated: true,
                    shadow: { sizeX: shadowSizeX, sizeY: shadowSizeY, offsetZ: - 0.1, alpha: 0.5 },
                    mass: 1.5,
                    soundName: 'woodHit'
                })
            }
        }

        // Area
        const boardsCount = this.imageSources.length
        const boardSpanWidth = boardsCount > 0 ? (boardsCount - 1) * this.boards.xInter : 0
        const interactionHalfExtentsX = Math.max(6.5 * this.scale, boardSpanWidth * 0.5 + 3.5 * this.scale)
        const interactionMinY = this.floor.y - 2.5
        const interactionMaxY = this.boards.y + 2.5
        const interactionCenterY = (interactionMinY + interactionMaxY) * 0.5
        const interactionHalfExtentsY = (interactionMaxY - interactionMinY) * 0.5

        this.floor.area = this.areas.add({
            position: new THREE.Vector2(this.x + this.boards.xStart + boardSpanWidth * 0.5, this.y + interactionCenterY * this.scale),
            halfExtents: new THREE.Vector2(interactionHalfExtentsX, interactionHalfExtentsY * this.scale)
        })
        this.floor.area.on('interact', () =>
        {
            this.openViewer()
        })

        // Area label
        this.floor.areaLabel = this.meshes.areaLabel.clone()
        this.floor.areaLabel.position.x = this.boards.xStart + boardSpanWidth * 0.5
        this.floor.areaLabel.position.y = (interactionCenterY - interactionHalfExtentsY + 0.9) * this.scale
        this.floor.areaLabel.position.z = 0.001
        this.floor.areaLabel.matrixAutoUpdate = false
        this.floor.areaLabel.updateMatrix()
        this.floor.container.add(this.floor.areaLabel)
    }

    createFloorTextTexture(_paragraphs)
    {
        const canvas = document.createElement('canvas')
        canvas.width = 2048
        canvas.height = 1024

        const context = canvas.getContext('2d')
        if(!context)
        {
            return this.floorTexture
        }

        const paragraphs = _paragraphs
            .map((_paragraph) => (_paragraph || '').trim())
            .filter((_paragraph) => _paragraph.length > 0)

        if(paragraphs.length === 0)
        {
            return this.floorTexture
        }

        const cardWidth = canvas.width * 0.86
        const cardX = (canvas.width - cardWidth) * 0.5
        const cardY = canvas.height * 0.12
        const cardHeight = canvas.height * 0.76
        const maxTextWidth = cardWidth * 0.78
        const paragraphGapRatio = 1.15
        const fontFamily = 'Arial, Helvetica, sans-serif'
        const accentColor = '#d8ff8a'
        const bodyColor = '#ffffff'
        const glowColor = 'rgba(216, 255, 138, 0.16)'

        const wrapLines = (_text, _lineWidth) =>
        {
            const words = _text.split(/\s+/)
            const lines = []
            let currentLine = ''

            for(const word of words)
            {
                const testLine = currentLine ? `${currentLine} ${word}` : word
                if(context.measureText(testLine).width <= _lineWidth || !currentLine)
                {
                    currentLine = testLine
                }
                else
                {
                    lines.push(currentLine)
                    currentLine = word
                }
            }

            if(currentLine)
            {
                lines.push(currentLine)
            }

            return lines
        }

        let fontSize = 54
        let lineHeight = Math.round(fontSize * 1.3)
        let paragraphsLines = []
        let totalTextHeight = 0

        while(fontSize >= 30)
        {
            context.font = `700 ${fontSize}px ${fontFamily}`
            lineHeight = Math.round(fontSize * 1.2)
            paragraphsLines = paragraphs.map((_paragraph) => wrapLines(_paragraph, maxTextWidth))

            const paragraphGap = Math.round(lineHeight * paragraphGapRatio)
            totalTextHeight = paragraphsLines.reduce((_sum, _lines) => _sum + _lines.length * lineHeight, 0) + (paragraphsLines.length - 1) * paragraphGap

            if(totalTextHeight <= cardHeight * 0.62)
            {
                break
            }

            fontSize -= 2
        }

        context.clearRect(0, 0, canvas.width, canvas.height)

        // Soft atmospheric glow
        const glowGradient = context.createRadialGradient(canvas.width * 0.22, canvas.height * 0.28, 30, canvas.width * 0.22, canvas.height * 0.28, canvas.width * 0.42)
        glowGradient.addColorStop(0, glowColor)
        glowGradient.addColorStop(1, 'rgba(216, 255, 138, 0)')
        context.fillStyle = glowGradient
        context.fillRect(0, 0, canvas.width, canvas.height)

        // Background card
        context.fillStyle = 'rgba(8, 26, 10, 0.22)'
        context.strokeStyle = 'rgba(216, 255, 138, 0.72)'
        context.lineWidth = 6
        this.drawRoundedRect(context, cardX, cardY, cardWidth, cardHeight, 42)
        context.fill()
        context.stroke()

        // Large decorative quote mark
        context.fillStyle = 'rgba(255, 255, 255, 0.08)'
        context.font = `700 220px ${fontFamily}`
        context.textAlign = 'left'
        context.textBaseline = 'top'
        context.fillText('"', cardX + 56, cardY + 118)

        // Accent bar
        context.fillStyle = accentColor
        this.drawRoundedRect(context, cardX + 70, cardY + 54, 260, 18, 9)
        context.fill()

        // Small label
        context.fillStyle = accentColor
        context.textAlign = 'left'
        context.textBaseline = 'middle'
        context.font = `700 42px ${fontFamily}`
        context.fillText('AGRICULTURAL TIMELINE', cardX + 70, cardY + 110)

        context.fillStyle = 'rgba(255, 255, 255, 0.52)'
        context.textAlign = 'right'
        context.font = `700 32px ${fontFamily}`
        context.fillText(this.name.toUpperCase(), cardX + cardWidth - 70, cardY + 110)

        // Divider
        context.strokeStyle = 'rgba(255, 255, 255, 0.18)'
        context.lineWidth = 3
        context.beginPath()
        context.moveTo(cardX + 70, cardY + 154)
        context.lineTo(cardX + cardWidth - 70, cardY + 154)
        context.stroke()

        context.textAlign = 'left'
        context.textBaseline = 'top'
        context.fillStyle = bodyColor
        context.font = `700 ${fontSize}px ${fontFamily}`

        const paragraphGap = Math.round(lineHeight * paragraphGapRatio)
        let drawY = cardY + 208

        paragraphsLines.forEach((lines, index) =>
        {
            const badgeX = cardX + 72
            const badgeY = drawY + 10
            const badgeSize = 62

            context.fillStyle = 'rgba(216, 255, 138, 0.14)'
            this.drawRoundedRect(context, badgeX, badgeY, badgeSize, badgeSize, 16)
            context.fill()

            context.strokeStyle = 'rgba(216, 255, 138, 0.55)'
            context.lineWidth = 2
            this.drawRoundedRect(context, badgeX, badgeY, badgeSize, badgeSize, 16)
            context.stroke()

            context.fillStyle = accentColor
            context.textAlign = 'center'
            context.textBaseline = 'middle'
            context.font = `700 ${Math.max(26, fontSize * 0.5)}px ${fontFamily}`
            context.fillText(`0${index + 1}`, badgeX + badgeSize * 0.5, badgeY + badgeSize * 0.56)

            context.fillStyle = bodyColor
            context.textAlign = 'left'
            context.textBaseline = 'top'
            context.font = `700 ${fontSize}px ${fontFamily}`

            for(const line of lines)
            {
                context.fillText(line, cardX + 170, drawY)
                drawY += lineHeight
            }

            drawY += paragraphGap
        })

        const texture = new THREE.CanvasTexture(canvas)
        texture.colorSpace = THREE.SRGBColorSpace
        texture.magFilter = THREE.LinearFilter
        texture.minFilter = THREE.LinearFilter
        texture.generateMipmaps = false
        texture.needsUpdate = true

        return texture
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
}
