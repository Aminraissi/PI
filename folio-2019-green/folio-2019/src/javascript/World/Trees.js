import * as THREE from 'three'

/**
 * Trees — arbres 3D réalistes pour une cité universitaire agricole.
 *
 * 3 types :
 *   0 = Chêne large  (branches, racines, feuillage multi-couche)
 *   1 = Peuplier élancé (feuillage conique empilé)
 *   2 = Arbuste touffu  (boules de feuillage multiples)
 *
 * Matcaps : green, emeraldGreen, brown
 * Animation : balancement au vent doux + micro-rafales
 */

export default class Trees
{
    constructor(_options)
    {
        this.time      = _options.time
        this.materials = _options.materials
        this.container = _options.container
        this.trees     = []

        this._buildMaterials()
        this._buildGeometryCache()
        this._removeOriginalGLBTrees()
        this._placeAllTrees()
        this._startWindAnimation()
    }

    _buildMaterials()
    {
        this.matGreen   = this.materials.shades.items.green
        this.matEmerald = this.materials.shades.items.emeraldGreen
        this.matBrown   = this.materials.shades.items.brown
    }

    _buildGeometryCache()
    {
        this.geo = {
            foliageLarge  : new THREE.IcosahedronGeometry(0.72, 1),
            foliageMedium : new THREE.IcosahedronGeometry(0.54, 1),
            foliageSmall  : new THREE.IcosahedronGeometry(0.38, 1),
            foliageTiny   : new THREE.IcosahedronGeometry(0.22, 0),
            coneL         : new THREE.ConeGeometry(0.52, 1.1, 7),
            coneM         : new THREE.ConeGeometry(0.38, 0.85, 6),
            coneS         : new THREE.ConeGeometry(0.26, 0.65, 6),
            trunkOak      : new THREE.CylinderGeometry(0.075, 0.14, 1.0, 8),
            trunkPoplar   : new THREE.CylinderGeometry(0.055, 0.10, 1.4, 7),
            trunkBush     : new THREE.CylinderGeometry(0.065, 0.11, 0.55, 7),
            root          : new THREE.ConeGeometry(0.09, 0.28, 4),
        }
    }

    _removeOriginalGLBTrees()
    {
        const toRemove = []
        this.container.traverse(child =>
        {
            if(child.isMesh && !child.userData.isCustomTree)
            {
                const n = child.name || ''
                if(/shadeGreen|shadeBrown/i.test(n))
                    toRemove.push(child)
            }
        })
        toRemove.forEach(m => { if(m.parent) m.parent.remove(m) })
    }

    _rng(seed)
    {
        let s = ((seed + 1) * 2654435761) | 0
        return () =>
        {
            s ^= s << 13; s ^= s >> 17; s ^= s << 5
            return (s >>> 0) / 4294967296
        }
    }

    _mesh(geo, mat)
    {
        const m = new THREE.Mesh(geo, mat)
        m.userData.isCustomTree = true
        return m
    }

    // ── Chêne large ──────────────────────────────────────────────────────────
    _makeOak(group, r)
    {
        const height  = 0.85 + r() * 0.35
        const crown   = 0.62 + r() * 0.32
        const matFol  = r() > 0.45 ? this.matEmerald : this.matGreen

        // Tronc
        const trunk = this._mesh(this.geo.trunkOak, this.matBrown)
        trunk.scale.y = height
        trunk.position.z = height * 0.5
        trunk.rotation.x = Math.PI / 2
        group.add(trunk)

        // Racines
        for(let i = 0; i < 3; i++)
        {
            const root = this._mesh(this.geo.root, this.matBrown)
            root.rotation.x = Math.PI / 2
            root.rotation.z = (i / 3) * Math.PI * 2 + r() * 0.5
            root.position.x = Math.cos((i / 3) * Math.PI * 2) * 0.11
            root.position.y = Math.sin((i / 3) * Math.PI * 2) * 0.11
            root.position.z = 0.1
            root.scale.setScalar(0.7)
            group.add(root)
        }

        // Branches latérales
        for(let i = 0; i < 2; i++)
        {
            const bg  = new THREE.CylinderGeometry(0.02, 0.045, 0.38, 5)
            const b   = this._mesh(bg, this.matBrown)
            const ang = (i / 2) * Math.PI + r() * 0.8
            b.rotation.z = (Math.PI / 2 - 0.6) * (i % 2 === 0 ? 1 : -1)
            b.rotation.y = ang
            b.position.set(Math.cos(ang) * 0.15, Math.sin(ang) * 0.15, height * 0.7)
            group.add(b)
        }

        // Feuillage — couches principales
        const baseZ = height + crown * 0.12
        const layers = [
            { geo: this.geo.foliageLarge,  z: baseZ,              s: crown,        ox: 0, oy: 0 },
            { geo: this.geo.foliageMedium, z: baseZ + crown*0.72, s: crown * 0.78, ox: 0, oy: 0 },
            { geo: this.geo.foliageSmall,  z: baseZ + crown*1.28, s: crown * 0.52, ox: 0, oy: 0 },
        ]

        // Clusters latéraux (2–3)
        const clusterCount = 2 + Math.floor(r() * 2)
        for(let i = 0; i < clusterCount; i++)
        {
            const a = (i / clusterCount) * Math.PI * 2 + r() * 1.2
            layers.push({
                geo: this.geo.foliageMedium,
                z: baseZ + crown * (0.1 + r() * 0.55),
                s: crown * (0.42 + r() * 0.22),
                ox: Math.cos(a) * crown * 0.45,
                oy: Math.sin(a) * crown * 0.45
            })
        }

        // Renforts texture (petites boules)
        for(let i = 0; i < 3; i++)
        {
            const a = r() * Math.PI * 2
            layers.push({
                geo: this.geo.foliageTiny,
                z: baseZ + r() * crown * 0.9,
                s: 0.9 + r() * 0.3,
                ox: Math.cos(a) * crown * 0.55,
                oy: Math.sin(a) * crown * 0.55
            })
        }

        layers.forEach(l =>
        {
            const m = this._mesh(l.geo, matFol)
            m.position.set(l.ox, l.oy, l.z)
            m.rotation.z = r() * Math.PI * 2
            m.rotation.x = (r() - 0.5) * 0.4
            m.scale.setScalar(l.s)
            group.add(m)
        })
    }

    // ── Peuplier élancé ───────────────────────────────────────────────────────
    _makePoplar(group, r)
    {
        const height = 1.1 + r() * 0.45
        const matFol = this.matGreen

        const trunk = this._mesh(this.geo.trunkPoplar, this.matBrown)
        trunk.scale.y = height
        trunk.position.z = height * 0.5
        trunk.rotation.x = Math.PI / 2
        group.add(trunk)

        const etages = [
            { geo: this.geo.coneL, z: height * 0.52, s: 0.9 + r() * 0.2 },
            { geo: this.geo.coneM, z: height * 0.73, s: 0.85 + r() * 0.15 },
            { geo: this.geo.coneS, z: height * 0.91, s: 0.8 + r() * 0.15 },
            { geo: new THREE.ConeGeometry(0.14, 0.45, 5), z: height * 1.07, s: 1.0 },
        ]

        etages.forEach(e =>
        {
            const m = this._mesh(e.geo, matFol)
            m.position.z = e.z
            m.rotation.x = Math.PI / 2
            m.rotation.z = r() * Math.PI * 2
            m.scale.setScalar(e.s)
            group.add(m)
        })

        // Petits clusters latéraux
        for(let i = 0; i < 3; i++)
        {
            const a = r() * Math.PI * 2
            const m = this._mesh(this.geo.foliageTiny, matFol)
            m.position.set(Math.cos(a) * 0.28, Math.sin(a) * 0.28, height * (0.5 + r() * 0.4))
            m.scale.setScalar(0.7 + r() * 0.5)
            group.add(m)
        }
    }

    // ── Arbuste touffu ────────────────────────────────────────────────────────
    _makeBush(group, r)
    {
        const height = 0.45 + r() * 0.25
        const crown  = 0.5  + r() * 0.3
        const matFol = r() > 0.5 ? this.matEmerald : this.matGreen

        const trunk = this._mesh(this.geo.trunkBush, this.matBrown)
        trunk.scale.y = height
        trunk.position.z = height * 0.45
        trunk.rotation.x = Math.PI / 2
        group.add(trunk)

        // Masse centrale
        const center = this._mesh(this.geo.foliageLarge, matFol)
        center.position.z = height + crown * 0.55
        center.scale.setScalar(crown)
        group.add(center)

        // Boules satellites
        const ballCount = 4 + Math.floor(r() * 4)
        for(let i = 0; i < ballCount; i++)
        {
            const geo = r() > 0.5 ? this.geo.foliageSmall : this.geo.foliageMedium
            const m   = this._mesh(geo, matFol)
            const a   = r() * Math.PI * 2
            const d   = r() * crown * 0.55
            m.position.set(Math.cos(a) * d, Math.sin(a) * d, height + crown * (0.1 + r() * 0.8))
            m.scale.setScalar(0.55 + r() * 0.55)
            m.rotation.z = r() * Math.PI * 2
            group.add(m)
        }
    }

    // ── Construction d'un arbre ───────────────────────────────────────────────
    _makeTree(variant, seed)
    {
        const r     = this._rng(seed)
        const type  = variant % 3
        const group = new THREE.Group()
        group.userData.isCustomTree = true

        if(type === 0)      this._makeOak(group, r)
        else if(type === 1) this._makePoplar(group, r)
        else                this._makeBush(group, r)

        group.userData.swayPhase = r() * Math.PI * 2
        group.userData.swaySpeed = 0.28 + r() * 0.32
        group.userData.swayAmp   = 0.012 + r() * 0.008

        return group
    }

    // ── Placement ─────────────────────────────────────────────────────────────
    _placeAllTrees()
    {
        // Section INTRO — all near-sign trees removed per user request
        this._spawnGroup([
            { x:  7.827, y: -10.107, v: 0 },
            { x: -7.431, y: -12.785, v: 2 },
        ], 0, 0)

        // Section INFORMATION (offset 1.2, -55)
        this._spawnGroup([
            { x:  7.008, y:  8.463,  v: 0 },
            { x: -8.839, y:  1.030,  v: 1 },
            { x: -6.336, y: -10.025, v: 2 },
        ], 1.2, -55)

        // Section PLAYGROUND (offset -38, -34)
        this._spawnGroup([
            { x:  -9.456, y:  9.912,  v: 0 },
            { x:  12.913, y:  8.404,  v: 1 },
            { x: -14.861, y:  7.928,  v: 2 },
            { x:  -3.197, y: -1.432,  v: 0 },
            { x:  15.163, y: -12.137, v: 1 },
            { x: -10.820, y: -4.513,  v: 2 },
        ], -38, -34)

        // Bordures de routes — peupliers alignés
        this._spawnGroup([
            { x:  5.6, y:  -7,  v: 1 }, { x: -5.6, y:  -7,  v: 1 },
            { x:  5.6, y: -13,  v: 1 }, { x: -5.6, y: -13,  v: 1 },
            { x:  5.6, y: -20,  v: 1 }, { x: -5.6, y: -20,  v: 1 },
            { x:  5.6, y: -26,  v: 1 }, { x: -5.6, y: -26,  v: 1 },
            { x:  10,  y: -29,  v: 1 }, { x:  10,  y: -31,  v: 1 },
            { x:  17,  y: -29,  v: 1 }, { x:  17,  y: -31,  v: 1 },
            { x:  23,  y: -29,  v: 1 }, { x:  23,  y: -31,  v: 1 },
            { x:  5.6, y: -36,  v: 1 }, { x: -5.6, y: -36,  v: 1 },
            { x:  5.6, y: -43,  v: 1 }, { x: -5.6, y: -43,  v: 1 },
            { x:  5.6, y: -50,  v: 1 }, { x: -5.6, y: -50,  v: 1 },
            { x: -12,  y: -29,  v: 1 }, { x: -12,  y: -31,  v: 1 },
            { x: -20,  y: -29,  v: 1 }, { x: -20,  y: -31,  v: 1 },
            { x: -28,  y: -29,  v: 1 }, { x: -28,  y: -31,  v: 1 },
        ], 0, 0)

        // Haies d'arbustes aux coins des sections
        this._spawnGroup([
            // Removed: visible in intro view  { x:  9.5,  y:  10,   v: 2 },
            // Removed: visible in intro view  { x: -9.5,  y:  10,  v: 2 },
            { x:  9.5,  y: -14,   v: 2 }, { x: -9.5,  y: -14,  v: 2 },
            { x: -40,   y: -22,   v: 2 }, { x: -40,   y: -44,  v: 2 },
            { x:  3,    y: -58,   v: 2 }, { x: -1.4,  y: -62,  v: 2 },
        ], 0, 0)
    }

    _spawnGroup(positions, offsetX, offsetY)
    {
        positions.forEach((pos, i) =>
        {
            const seed = i * 7919 + Math.floor(Math.abs(offsetX) * 13 + Math.abs(offsetY) * 7)
            const r    = this._rng(seed)
            const jx   = (r() - 0.5) * 0.25
            const jy   = (r() - 0.5) * 0.25

            const tree = this._makeTree(pos.v !== undefined ? pos.v : i, seed)
            tree.position.set(pos.x + offsetX + jx, pos.y + offsetY + jy, 0)
            tree.rotation.x = (r() - 0.5) * 0.06
            tree.rotation.y = (r() - 0.5) * 0.06
            tree.rotation.z = r() * Math.PI * 2

            this.container.add(tree)
            this.trees.push(tree)
        })
    }

    // ── Animation vent ────────────────────────────────────────────────────────
    _startWindAnimation()
    {
        this.time.on('tick', () =>
        {
            const t = this.time.elapsed * 0.001
            this.trees.forEach(tree =>
            {
                const ph  = tree.userData.swayPhase
                const sp  = tree.userData.swaySpeed
                const amp = tree.userData.swayAmp
                tree.rotation.x  = Math.sin(t * sp       + ph) * amp
                tree.rotation.y  = Math.sin(t * sp * 0.6 + ph + 1.3) * amp * 0.7
                tree.rotation.x += Math.sin(t * sp * 3.1 + ph * 2)   * amp * 0.18
            })
        })
    }
}
