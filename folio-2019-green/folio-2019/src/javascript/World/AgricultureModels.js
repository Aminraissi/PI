import * as THREE from 'three'

export default class AgricultureModels
{
    static getModels(_materials, _items)
    {
        const models = []
        const mat = _materials.items.shadeAgriculture || _materials.items.yellow

        const names = [
            'agricultureTractor',
            'agricultureFarmer',
            'agricultureWheelbarrow',
            'agricultureLoader',
            'agricultureTrailer'
        ]

        for(const name of names)
        {
            if(_items && _items[name] && _items[name].scene)
            {
                const model = _items[name].scene.clone()
                
                // Fix materials: Scene lacks PBR lights, so Standard materials show up black.
                model.traverse(node => {
                    if (node.isMesh) {
                        node.castShadow = true
                        node.receiveShadow = true
                        
                        if(node.material) {
                            // Extract existing maps and color
                            const map = node.material.map
                            const color = node.material.color
                            
                            // Convert to a material that doesn't need light to be visible
                            const newMat = new THREE.MeshBasicMaterial({
                                map: map || null,
                                color: color || 0xffffff,
                                side: THREE.DoubleSide
                            })
                            node.material = newMat
                        }
                    }
                })

                // Normalize scale so it fits nicely on a pedestal (max dim = 1.5 approx)
                const boundingBox = new THREE.Box3().setFromObject(model)
                const size = new THREE.Vector3()
                boundingBox.getSize(size)
                
                const maxDim = Math.max(size.x, size.y, size.z)
                const targetSize = 1.6 // Arbitrary size that looks good
                const scale = maxDim > 0 ? (targetSize / maxDim) : 1
                
                model.scale.set(scale, scale, scale)
                
                // Center it horizontally
                const center = new THREE.Vector3()
                boundingBox.getCenter(center)
                model.position.x -= center.x * scale
                model.position.y -= center.y * scale

                // Sit exactly on ground (z=0)
                model.position.z -= boundingBox.min.z * scale

                // We wrap it in a group to hold these offset/scale properties securely
                const group = new THREE.Group()
                group.add(model)
                models.push(group)
            }
            else
            {
                // Fallback geometry if the GLB is missing or hasn't loaded properly
                console.warn(`Model ${name} was not loaded properly. Using fallback.`)
                const fallback = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.8, 0.8), mat)
                fallback.castShadow = true
                fallback.receiveShadow = true
                fallback.position.z = 0.4
                models.push(fallback)
            }
        }

        return models
    }
}
