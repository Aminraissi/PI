import * as THREE from 'three'
import Project from './Project'
import ProjectBoardMaterial from '../../Materials/ProjectBoard.js'
import gsap from 'gsap'

export default class ProjectsSection
{
    constructor(_options)
    {
        // Options
        this.time = _options.time
        this.resources = _options.resources
        this.camera = _options.camera
        this.passes = _options.passes
        this.objects = _options.objects
        this.areas = _options.areas
        this.zones = _options.zones
        this.tiles = _options.tiles
        this.debug = _options.debug
        this.x = _options.x
        this.y = _options.y

        // Debug
        if(this.debug)
        {
            this.debugFolder = this.debug.addFolder('projects')
            this.debugFolder.open()
        }

        // Set up
        this.items = []

        this.interDistance = 24
        this.positionRandomess = 5
        this.projectHalfWidth = 9

        this.container = new THREE.Object3D()
        this.container.matrixAutoUpdate = false
        this.container.updateMatrix()

        this.setGeometries()
        this.setMeshes()
        this.setList()
        this.setFinalShowcaseConfig()
        this.setZone()

        // Add all project from the list
        for(const _options of this.list)
        {
            this.add(_options)
        }

        this.setFinalShowcase()
    }

    setGeometries()
    {
        this.geometries = {}
        this.geometries.floor = new THREE.PlaneGeometry(16, 8)
    }

    setMeshes()
    {
        this.meshes = {}

        // this.meshes.boardStructure = this.objects.getConvertedMesh(this.resources.items.projectsBoardStructure.scene.children, { floorShadowTexture: this.resources.items.projectsBoardStructureFloorShadowTexture })
        this.resources.items.areaOpenTexture.magFilter = THREE.NearestFilter
        this.resources.items.areaOpenTexture.minFilter = THREE.LinearFilter
        this.meshes.boardPlane = this.resources.items.projectsBoardPlane.scene.children[0]
        this.meshes.areaLabel = new THREE.Mesh(new THREE.PlaneGeometry(2, 0.5), new THREE.MeshBasicMaterial({ transparent: true, depthWrite: false, color: 0xffffff, alphaMap: this.resources.items.areaOpenTexture }))
        this.meshes.areaLabel.matrixAutoUpdate = false
    }

    setList()
    {
        this.list = [
            {
                name: 'Three.js Journey',
                imageSources:
                [
                    './models/projects/threejsJourney/slideA.webp',
                    './models/projects/threejsJourney/slideB.webp',
                    './models/projects/threejsJourney/slideC.webp',
                    './models/projects/threejsJourney/slideD.webp'
                ],
                floorTexture: this.resources.items.projectsThreejsJourneyFloorTexture,
                floorTextParagraphs:
                [
                    'Around 10,000 BC, humans in the Fertile Crescent began cultivating wheat and barley, shifting from hunting and gathering to permanent settlements.',
                    'By domesticating animals like oxen and inventing the plow, early farmers increased food production, enabling the rise of the first cities in Mesopotamia.'
                ],
                link:
                {
                    href: 'https://threejs-journey.com?c=p3',
                    x: - 4.8,
                    y: - 3,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'fwa', x: 3.95, y: 4.15 }
                ]
            },
            {
                name: 'Chartogne Taillet',
                imageSources:
                [
                    './models/projects/chartogne/slideA.jpg',
                    './models/projects/chartogne/slideB.jpg',
                    './models/projects/chartogne/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsChartogneFloorTexture,
                floorTextParagraphs:
                [
                    'As farming spread, river societies learned to guide water with canals and dikes, turning seasonal floods into reliable harvests and stabilizing food supplies.',
                    'Granaries, the wheel, and written records helped communities store grain, move harvests, and count taxes, making agriculture the backbone of the first cities.'
                ],
                link:
                {
                    href: 'https://chartogne-taillet.com',
                    x: - 4.8,
                    y: - 3.3,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                    { type: 'cssda', x: 7.2, y: 4.15 }
                ]
            },
            {
                name: 'Bonhomme | 10 ans',
                imageSources:
                [
                    './models/projects/bonhomme10ans/slideA.webp',
                    './models/projects/bonhomme10ans/slideB.webp',
                    './models/projects/bonhomme10ans/slideC.webp',
                    './models/projects/bonhomme10ans/slideD.webp'
                ],
                floorTexture: this.resources.items.projectsBonhomme10ansFloorTexture,
                floorTextParagraphs:
                [
                    'Over time, farmers improved the plow, shaped hills into terraces, and adapted crops to dry slopes, wetlands, and river valleys.',
                    'Animal traction and stronger tools increased the area a family could cultivate, while specialized landscapes like vineyards and orchards tied farming to local climates.'
                ],
                link:
                {
                    href: 'https://anniversary.bonhommeparis.com/',
                    x: - 4.8,
                    y: - 2,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                ]
            },
            {
                name: 'Luni.app',
                imageSources:
                [
                    './models/projects/luni/slideA.webp',
                    './models/projects/luni/slideB.webp',
                    './models/projects/luni/slideC.webp',
                    './models/projects/luni/slideD.webp'
                ],
                floorTexture: this.resources.items.projectsLuniFloorTexture,
                floorTextParagraphs:
                [
                    'In medieval agriculture, the three-field system reduced exhaustion of the soil by rotating crops and leaving only part of the land to rest.',
                    'Watermills and windmills mechanized grinding, while better crop rotation steadily improved yields, nutrition, and resilience against poor seasons.'
                ],
                link:
                {
                    href: 'https://luni.app',
                    x: - 4.8,
                    y: - 3,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                ]
            },
            {
                name: 'Madbox',
                imageSources:
                [
                    './models/projects/madbox/slideA.jpg',
                    './models/projects/madbox/slideB.jpg',
                    './models/projects/madbox/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsMadboxFloorTexture,
                floorTextParagraphs:
                [
                    'Early modern agriculture became more scientific as farmers tested seed selection, observed soils more carefully, and designed tools to place seed with greater regularity.',
                    'The seed drill, studies of soil, and selective breeding turned farming from inherited habit into a field increasingly guided by experiment and measurement.'
                ],
                link:
                {
                    href: 'https://madbox.io',
                    x: - 4.8,
                    y: - 4,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 }
                ]
            },
            {
                name: 'Scout',
                imageSources:
                [
                    './models/projects/scout/slideA.jpg',
                    './models/projects/scout/slideB.jpg',
                    './models/projects/scout/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsScoutFloorTexture,
                floorTextParagraphs:
                [
                    'The industrial age transformed agriculture through threshing machines, mechanical reapers, steam traction, and eventually tractors.',
                    'Tasks that once required many hands could now be completed faster and over larger fields, pushing farms toward mechanization and commercial scale.'
                ],
                link:
                {
                    href: 'https://fromscout.com',
                    x: - 4.8,
                    y: - 2,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                ]
            },
            // {
            //     name: 'Zenly',
            //     imageSources:
            //     [
            //         './models/projects/zenly/slideA.jpg',
            //         './models/projects/zenly/slideB.jpg',
            //         './models/projects/zenly/slideC.jpg'
            //     ],
            //     floorTexture: this.resources.items.projectsZenlyFloorTexture,
            //     link:
            //     {
            //         href: 'https://zen.ly',
            //         x: - 4.8,
            //         y: - 4.2,
            //         halfExtents:
            //         {
            //             x: 3.2,
            //             y: 1.5
            //         }
            //     },
            //     distinctions:
            //     [
            //         { type: 'awwwards', x: 3.95, y: 4.15 },
            //         { type: 'fwa', x: 5.6, y: 4.15 },
            //         { type: 'cssda', x: 7.2, y: 4.15 }
            //     ]
            // },
            {
                name: 'priorHoldings',
                imageSources:
                [
                    './models/projects/priorHoldings/slideA.jpg',
                    './models/projects/priorHoldings/slideB.jpg',
                    './models/projects/priorHoldings/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsPriorHoldingsFloorTexture,
                floorTextParagraphs:
                [
                    'In the late nineteenth and early twentieth centuries, chemistry and cold storage changed what agriculture could produce, preserve, and transport.',
                    'Synthetic nitrogen from the Haber process boosted plant growth, while refrigeration extended the life of milk, meat, fruit, and vegetables beyond the farm gate.'
                ],
                link:
                {
                    href: 'https://prior.co.jp/discover/',
                    x: - 4.8,
                    y: - 3,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                    { type: 'cssda', x: 7.2, y: 4.15 }
                ]
            },
            {
                name: 'orano',
                imageSources:
                [
                    './models/projects/orano/slideA.jpg',
                    './models/projects/orano/slideB.jpg',
                    './models/projects/orano/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsOranoFloorTexture,
                floorTextParagraphs:
                [
                    'During the twentieth century, combines united cutting, threshing, and cleaning in a single machine, dramatically increasing harvesting speed.',
                    'At the same time, the Green Revolution spread improved varieties, irrigation, and inputs, lifting yields in many regions while also raising new environmental questions.'
                ],
                link:
                {
                    href: 'https://orano.imm-g-prod.com/experience/innovation/en',
                    x: - 4.8,
                    y: - 3.4,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                    { type: 'cssda', x: 7.2, y: 4.15 }
                ]
            },
            {
                name: 'citrixRedbull',
                imageSources:
                [
                    './models/projects/citrixRedbull/slideA.jpg',
                    './models/projects/citrixRedbull/slideB.jpg',
                    './models/projects/citrixRedbull/slideC.jpg'
                ],
                floorTexture: this.resources.items.projectsCitrixRedbullFloorTexture,
                floorTextParagraphs:
                [
                    'Late modern agriculture became data-driven as center-pivot irrigation, plant breeding, and satellite positioning helped farmers manage fields more precisely.',
                    'With remote sensing and digital mapping, decisions about water, seed, and fertilizer could be adjusted parcel by parcel instead of treating every hectare the same way.'
                ],
                link:
                {
                    href: 'https://thenewmobileworkforce.imm-g-prod.com/',
                    x: - 4.8,
                    y: - 4.4,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                    { type: 'cssda', x: 7.2, y: 4.15 }
                ]
            },
            {
                name: 'gleecChat',
                imageSources:
                [
                    './models/projects/gleecChat/slideA.jpg',
                    './models/projects/gleecChat/slideB.jpg',
                    './models/projects/gleecChat/slideC.jpg',
                    './models/projects/gleecChat/slideD.jpg'
                ],
                floorTexture: this.resources.items.projectsGleecChatFloorTexture,
                floorTextParagraphs:
                [
                    'Today, agriculture is entering a new phase in which sensors, genome editing, vertical farms, and precision software aim to produce more with fewer resources.',
                    'Robots, connected machines, and controlled environments are turning the farm into a living innovation map, where each new tool responds to climate, soil, and food demand.'
                ],
                link:
                {
                    href: 'http://gleec.imm-g-prod.com',
                    x: - 4.8,
                    y: - 3.4,
                    halfExtents:
                    {
                        x: 3.2,
                        y: 1.5
                    }
                },
                distinctions:
                [
                    { type: 'awwwards', x: 3.95, y: 4.15 },
                    { type: 'fwa', x: 5.6, y: 4.15 },
                    { type: 'cssda', x: 7.2, y: 4.15 }
                ]
            },
            // {
            //     name: 'keppler',
            //     imageSources:
            //     [
            //         './models/projects/keppler/slideA.jpg',
            //         './models/projects/keppler/slideB.jpg',
            //         './models/projects/keppler/slideC.jpg'
            //     ],
            //     floorTexture: this.resources.items.projectsKepplerFloorTexture,
            //     link:
            //     {
            //         href: 'https://brunosimon.github.io/keppler/',
            //         x: 2.75,
            //         y: - 1.1,
            //         halfExtents:
            //         {
            //             x: 3.2,
            //             y: 1.5
            //         }
            //     },
            //     distinctions: []
            // }
        ]
    }

    setZone()
    {
        const totalWidth = (this.list.length + 10) * (this.interDistance / 2)

        const zone = this.zones.add({
            position: { x: this.x + totalWidth - this.projectHalfWidth - 6, y: this.y + 8 },
            halfExtents: { x: totalWidth, y: 22 },
            data: { cameraAngle: 'projects' }
        })

        zone.on('in', (_data) =>
        {
            this.camera.angle.set(_data.cameraAngle)
            gsap.to(this.passes.horizontalBlurPass.material.uniforms.uStrength.value, { x: 0, duration: 2 })
            gsap.to(this.passes.verticalBlurPass.material.uniforms.uStrength.value, { y: 0, duration: 2 })
        })

        zone.on('out', () =>
        {
            this.camera.angle.set('default')
            gsap.to(this.passes.horizontalBlurPass.material.uniforms.uStrength.value, { x: this.passes.horizontalBlurPass.strength, duration: 2 })
            gsap.to(this.passes.verticalBlurPass.material.uniforms.uStrength.value, { y: this.passes.verticalBlurPass.strength, duration: 2 })
        })
    }

    setFinalShowcaseConfig()
    {
        const lastProject = this.items[this.items.length - 1]

        this.finalShowcase = {
            x: lastProject ? lastProject.x + 2.5 : this.x + this.list.length * this.interDistance,
            y: lastProject ? lastProject.y + 5
                : this.y + 5,
            scale: 1.35,
            carousel: false,
            carouselDelay: 2.6,
            focusOnInteract: true,
            focusOnlyOnInteract: true,
            focusAngle: 'projectsClose',
            focusZoom: 0,
            focusTargetZ: 1.7,
            name: 'Agri Film',
            imageSources: [
                './models/projects/finale/slideA.jpg',
                './models/projects/finale/slideB.jpg',
                './models/projects/finale/slideC.jpg',
                './models/projects/finale/slideD.jpg',
                './models/projects/finale/slideE.jpg',
                './models/projects/finale/slideF.jpg'
            ],
            floorTextParagraphs: [
                'Beyond the timeline, agriculture becomes a living landscape of beauty, technology, water, light, and biodiversity.',
                'From terraced mountains to greenhouses and new energy fields, this final scene imagines farming as a cinematic ecosystem in constant transformation.'
            ],
            floorTexture: this.resources.items.projectsThreejsJourneyFloorTexture,
            link:
            {
                href: '#',
                x: - 4.8,
                y: - 3,
                halfExtents:
                {
                    x: 3.2,
                    y: 1.5
                }
            },
            distinctions: []
        }
    }

    add(_options)
    {
        const x = this.x + this.items.length * this.interDistance
        let y = this.y
        if(this.items.length > 0)
        {
            y += (Math.random() - 0.5) * this.positionRandomess
        }

        // Create project
        const project = new Project({
            time: this.time,
            resources: this.resources,
            objects: this.objects,
            areas: this.areas,
            camera: this.camera,
            geometries: this.geometries,
            meshes: this.meshes,
            debug: this.debugFolder,
            x: x,
            y: y,
            ..._options
        })

        this.container.add(project.container)

        // Add tiles
        if(this.items.length >= 1)
        {
            const previousProject = this.items[this.items.length - 1]
            const start = new THREE.Vector2(previousProject.x + this.projectHalfWidth, previousProject.y)
            const end = new THREE.Vector2(project.x - this.projectHalfWidth, project.y)
            const delta = end.clone().sub(start)
            this.tiles.add({
                start: start,
                delta: delta
            })
        }

        // Save
        this.items.push(project)
    }

    setFinalShowcase()
    {
        const showcase = new Project({
            time: this.time,
            resources: this.resources,
            objects: this.objects,
            areas: this.areas,
            camera: this.camera,
            geometries: this.geometries,
            meshes: this.meshes,
            debug: this.debugFolder,
            x: this.finalShowcase.x,
            y: this.finalShowcase.y,
            scale: this.finalShowcase.scale,
            ...this.finalShowcase
        })

        this.container.add(showcase.container)

        const previousProject = this.items[this.items.length - 1]
        if(previousProject)
        {
            const scaledHalfWidth = this.projectHalfWidth * this.finalShowcase.scale
            const start = new THREE.Vector2(previousProject.x + this.projectHalfWidth, previousProject.y)
            const end = new THREE.Vector2(this.finalShowcase.x - scaledHalfWidth, this.finalShowcase.y)
            this.tiles.add({
                start,
                delta: end.clone().sub(start)
            })
        }
    }

    createFinaleTextTexture(_paragraphs)
    {
        const canvas = document.createElement('canvas')
        canvas.width = 2048
        canvas.height = 1024
        const context = canvas.getContext('2d')

        if(!context)
        {
            return this.resources.items.projectsThreejsJourneyFloorTexture
        }

        context.clearRect(0, 0, canvas.width, canvas.height)
        context.fillStyle = '#ffffff'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        context.font = 'italic 700 94px Arial'

        const lines = []
        const maxWidth = canvas.width * 0.78
        for(const paragraph of _paragraphs)
        {
            const words = paragraph.split(/\s+/)
            let currentLine = ''
            for(const word of words)
            {
                const testLine = currentLine ? `${currentLine} ${word}` : word
                if(context.measureText(testLine).width <= maxWidth || !currentLine)
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
            lines.push('')
        }

        const filteredLines = lines[lines.length - 1] === '' ? lines.slice(0, -1) : lines
        const lineHeight = 112
        const totalHeight = (filteredLines.length - 1) * lineHeight
        let y = canvas.height * 0.5 - totalHeight * 0.5

        for(const line of filteredLines)
        {
            if(line === '')
            {
                y += lineHeight * 0.45
                continue
            }

            context.fillText(line, canvas.width * 0.5, y)
            y += lineHeight
        }

        return new THREE.CanvasTexture(canvas)
    }

    createArrowTexture(_direction)
    {
        const canvas = document.createElement('canvas')
        canvas.width = 256
        canvas.height = 256
        const context = canvas.getContext('2d')

        if(!context)
        {
            return this.resources.items.areaOpenTexture
        }

        context.clearRect(0, 0, canvas.width, canvas.height)
        context.fillStyle = '#ffffff'
        context.beginPath()

        if(_direction === 'left')
        {
            context.moveTo(58, 128)
            context.lineTo(170, 48)
            context.lineTo(170, 92)
            context.lineTo(226, 92)
            context.lineTo(226, 164)
            context.lineTo(170, 164)
            context.lineTo(170, 208)
        }
        else
        {
            context.moveTo(198, 128)
            context.lineTo(86, 48)
            context.lineTo(86, 92)
            context.lineTo(30, 92)
            context.lineTo(30, 164)
            context.lineTo(86, 164)
            context.lineTo(86, 208)
        }

        context.closePath()
        context.fill()

        return new THREE.CanvasTexture(canvas)
    }

    createFinaleTitleTexture(_title)
    {
        const canvas = document.createElement('canvas')
        canvas.width = 1024
        canvas.height = 256
        const context = canvas.getContext('2d')

        if(!context)
        {
            return this.resources.items.areaOpenTexture
        }

        context.clearRect(0, 0, canvas.width, canvas.height)
        context.fillStyle = '#ffffff'
        context.textAlign = 'center'
        context.textBaseline = 'middle'
        context.font = '700 116px Comic Neue'
        context.fillText(_title, canvas.width * 0.5, canvas.height * 0.5)

        return new THREE.CanvasTexture(canvas)
    }
}
