package com.resquex.app.routing

import com.resquex.app.data.Hospital
import org.osmdroid.util.GeoPoint
import java.util.PriorityQueue
import kotlin.math.*

data class RoadNode(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
}

data class RoadEdge(
    val target: RoadNode,
    val roadName: String,
    val distanceKm: Double
)

class EmergencyRoadGraph {

    private val nodes = mutableMapOf<String, RoadNode>()
    private val adjacencyList = mutableMapOf<String, MutableList<RoadEdge>>()

    init {
        buildKeralaCorridorNetwork()
    }

    companion object {
        /**
         * Pure mathematical Haversine distance formula.
         * Runs 100% offline on any JVM/Android device without relying on android.location framework.
         */
        fun computeDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadiusKm = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusKm * c
        }
    }

    private fun addNode(id: String, name: String, lat: Double, lng: Double): RoadNode {
        val node = RoadNode(id, name, lat, lng)
        nodes[id] = node
        adjacencyList.putIfAbsent(id, mutableListOf())
        return node
    }

    private fun addBidirectionalEdge(fromId: String, toId: String, roadName: String) {
        val nodeA = nodes[fromId] ?: return
        val nodeB = nodes[toId] ?: return

        val distKm = computeDistanceKm(nodeA.latitude, nodeA.longitude, nodeB.latitude, nodeB.longitude)

        adjacencyList[fromId]?.add(RoadEdge(nodeB, roadName, distKm))
        adjacencyList[toId]?.add(RoadEdge(nodeA, roadName, distKm))
    }

    private fun buildKeralaCorridorNetwork() {
        // --- NH 66 Coastal Trunk Highway (North to South) ---
        addNode("kasaragod", "Kasaragod Town", 12.5000, 74.9900)
        addNode("kanhangad", "Kanhangad Junction", 12.3090, 75.0920)
        addNode("payyanur", "Payyanur Bypass", 12.1008, 75.2014)
        addNode("taliparamba", "Taliparamba Highway", 12.0465, 75.3582)
        addNode("kannur", "Kannur City Junction", 11.8745, 75.3704)
        addNode("thalassery", "Thalassery Junction", 11.7482, 75.4893)
        addNode("vadakara", "Vadakara Highway", 11.6034, 75.5902)
        addNode("koyilandy", "Koyilandy Bypass", 11.4412, 75.6983)
        addNode("kozhikode", "Kozhikode Beach / NH66", 11.2588, 75.7804)
        addNode("ramanattukara", "Ramanattukara Junction", 11.1780, 75.8670)
        addNode("tirur", "Tirur Highway Junction", 10.9130, 75.9224)
        addNode("ponnani", "Ponnani Junction", 10.7765, 75.9255)
        addNode("chavakkad", "Chavakkad Highway", 10.5898, 76.0253)
        addNode("kodungallur", "Kodungallur Bypass", 10.2290, 76.1971)
        addNode("north_paravur", "North Paravur Junction", 10.1478, 76.2281)
        addNode("edappally", "Edappally Toll (NH66/NH544)", 10.0260, 76.3125)
        addNode("ernakulam", "Ernakulam Marine Drive", 9.9816, 76.2811)
        addNode("kundannoor", "Kundannoor Junction", 9.9360, 76.3190)
        addNode("cherthala", "Cherthala Bypass", 9.6847, 76.3331)
        addNode("alappuzha", "Alappuzha Town NH66", 9.4981, 76.3388)
        addNode("vandanam", "Vandanam TDMC NH66", 9.4795, 76.3503)
        addNode("haripad", "Haripad Junction", 9.2880, 76.4620)
        addNode("kayamkulam", "Kayamkulam Junction", 9.1730, 76.5010)
        addNode("karunagappally", "Karunagappally NH66", 9.0560, 76.5367)
        addNode("chavara", "Chavara Bridge", 8.9950, 76.5390)
        addNode("kollam", "Kollam Asramam Junction", 8.8911, 76.5936)
        addNode("parippally", "Parippally GMC Junction", 8.8782, 76.7323)
        addNode("attingal", "Attingal Junction", 8.6950, 76.8140)
        addNode("kazhakkoottam", "Kazhakkoottam Technopark", 8.5680, 76.8730)
        addNode("tvm_city", "Thiruvananthapuram City", 8.4975, 76.9472)
        addNode("tvm_gmc", "Ulloor Trivandrum GMC", 8.5204, 76.9157)

        // Connect NH 66 Spine
        addBidirectionalEdge("kasaragod", "kanhangad", "NH 66")
        addBidirectionalEdge("kanhangad", "payyanur", "NH 66")
        addBidirectionalEdge("payyanur", "taliparamba", "NH 66")
        addBidirectionalEdge("taliparamba", "kannur", "NH 66")
        addBidirectionalEdge("kannur", "thalassery", "NH 66")
        addBidirectionalEdge("thalassery", "vadakara", "NH 66")
        addBidirectionalEdge("vadakara", "koyilandy", "NH 66")
        addBidirectionalEdge("koyilandy", "kozhikode", "NH 66")
        addBidirectionalEdge("kozhikode", "ramanattukara", "NH 66 Calicut Bypass")
        addBidirectionalEdge("ramanattukara", "tirur", "NH 66")
        addBidirectionalEdge("tirur", "ponnani", "NH 66")
        addBidirectionalEdge("ponnani", "chavakkad", "NH 66")
        addBidirectionalEdge("chavakkad", "kodungallur", "NH 66")
        addBidirectionalEdge("kodungallur", "north_paravur", "NH 66")
        addBidirectionalEdge("north_paravur", "edappally", "NH 66")
        addBidirectionalEdge("edappally", "ernakulam", "Banerji Road")
        addBidirectionalEdge("edappally", "kundannoor", "Kochi Bypass NH 66")
        addBidirectionalEdge("kundannoor", "cherthala", "NH 66")
        addBidirectionalEdge("cherthala", "alappuzha", "NH 66")
        addBidirectionalEdge("alappuzha", "vandanam", "NH 66")
        addBidirectionalEdge("vandanam", "haripad", "NH 66")
        addBidirectionalEdge("haripad", "kayamkulam", "NH 66")
        addBidirectionalEdge("kayamkulam", "karunagappally", "NH 66")
        addBidirectionalEdge("karunagappally", "chavara", "NH 66")
        addBidirectionalEdge("chavara", "kollam", "NH 66")
        addBidirectionalEdge("kollam", "parippally", "NH 66")
        addBidirectionalEdge("parippally", "attingal", "NH 66")
        addBidirectionalEdge("attingal", "kazhakkoottam", "NH 66")
        addBidirectionalEdge("kazhakkoottam", "tvm_gmc", "NH 66 Bypass to Ulloor")
        addBidirectionalEdge("tvm_gmc", "tvm_city", "Medical College Road")

        // --- NH 544 (Salem - Kochi Central Corridor) ---
        addNode("palakkad", "Palakkad Town", 10.7867, 76.6548)
        addNode("alathur", "Alathur Highway", 10.6457, 76.5452)
        addNode("vadakkencherry", "Vadakkencherry Junction", 10.5960, 76.4950)
        addNode("mannuthy", "Mannuthy Thrissur Bypass", 10.5360, 76.2620)
        addNode("thrissur_city", "Thrissur Swaraj Round", 10.5276, 76.2144)
        addNode("chalakudy", "Chalakudy Junction", 10.3070, 76.3330)
        addNode("angamaly", "Angamaly Junction", 10.1960, 76.3860)
        addNode("aluva", "Aluva Flyover", 10.1119, 76.3533)
        addNode("kalamassery", "Kalamassery GMC", 10.0163, 76.3516)

        addBidirectionalEdge("palakkad", "alathur", "NH 544")
        addBidirectionalEdge("alathur", "vadakkencherry", "NH 544")
        addBidirectionalEdge("vadakkencherry", "mannuthy", "NH 544 Kuthiran Expressway")
        addBidirectionalEdge("mannuthy", "thrissur_city", "Palakkad Road")
        addBidirectionalEdge("mannuthy", "chalakudy", "NH 544")
        addBidirectionalEdge("chalakudy", "angamaly", "NH 544")
        addBidirectionalEdge("angamaly", "aluva", "NH 544")
        addBidirectionalEdge("aluva", "kalamassery", "NH 544")
        addBidirectionalEdge("kalamassery", "edappally", "NH 544")

        // Thrissur to Kodungallur & Wadakkanchery
        addNode("wadakkanchery", "Wadakkanchery Hospital Junction", 10.6724, 76.2201)
        addNode("irinjalakuda", "Irinjalakuda Town", 10.3479, 76.2082)
        addBidirectionalEdge("thrissur_city", "wadakkanchery", "Shoranur Road")
        addBidirectionalEdge("thrissur_city", "irinjalakuda", "SH 22")
        addBidirectionalEdge("irinjalakuda", "kodungallur", "SH 22")

        // --- SH 1 / Main Central (MC) Road (Angamaly to Trivandrum) ---
        addNode("perumbavoor", "Perumbavoor Junction", 10.1130, 76.4740)
        addNode("muvattupuzha", "Muvattupuzha MC Road", 9.9856, 76.5819)
        addNode("koothattukulam", "Koothattukulam", 9.8630, 76.5920)
        addNode("ettumanoor", "Ettumanoor Junction", 9.6690, 76.5620)
        addNode("kottayam", "Kottayam Collectorate", 9.5916, 76.5222)
        addNode("kottayam_gmc", "Gandhinagar Kottayam GMC", 9.5888, 76.5367)
        addNode("changanassery", "Changanassery Town", 9.4442, 76.5417)
        addNode("thiruvalla", "Thiruvalla SCS Junction", 9.3837, 76.5741)
        addNode("chengannur", "Chengannur Station", 9.3175, 76.6083)
        addNode("kozhencherry", "Kozhencherry Bridge", 9.3392, 76.6853)
        addNode("pathanamthitta", "Pathanamthitta Town", 9.2647, 76.7872)
        addNode("konni", "Konni GMC Junction", 9.2486, 76.8582)
        addNode("adoor", "Adoor Central Toll", 9.1578, 76.7369)
        addNode("kottarakkara", "Kottarakkara Pulamon", 8.9986, 76.7720)
        addNode("venjaramoodu", "Venjaramoodu Junction", 8.6750, 76.9150)

        addBidirectionalEdge("angamaly", "perumbavoor", "MC Road SH 1")
        addBidirectionalEdge("perumbavoor", "muvattupuzha", "MC Road SH 1")
        addBidirectionalEdge("muvattupuzha", "koothattukulam", "MC Road SH 1")
        addBidirectionalEdge("koothattukulam", "ettumanoor", "MC Road SH 1")
        addBidirectionalEdge("ettumanoor", "kottayam_gmc", "Medical College Bypass")
        addBidirectionalEdge("kottayam_gmc", "kottayam", "Gandhinagar Road")
        addBidirectionalEdge("kottayam", "changanassery", "MC Road SH 1")
        addBidirectionalEdge("changanassery", "thiruvalla", "MC Road SH 1")
        addBidirectionalEdge("thiruvalla", "chengannur", "MC Road SH 1")
        addBidirectionalEdge("chengannur", "kozhencherry", "SH 7")
        addBidirectionalEdge("kozhencherry", "pathanamthitta", "SH 7")
        addBidirectionalEdge("pathanamthitta", "konni", "Main Eastern Highway")
        addBidirectionalEdge("chengannur", "adoor", "MC Road SH 1")
        addBidirectionalEdge("adoor", "pathanamthitta", "KP Road")
        addBidirectionalEdge("adoor", "kottarakkara", "MC Road SH 1")
        addBidirectionalEdge("kottarakkara", "venjaramoodu", "MC Road SH 1")
        addBidirectionalEdge("venjaramoodu", "tvm_city", "MC Road SH 1")
        addBidirectionalEdge("kottarakkara", "kollam", "NH 744")

        // --- Wayanad Corridor (NH 766) ---
        addNode("thamarassery", "Thamarassery Churam Base", 11.4172, 75.9332)
        addNode("vythiri", "Vythiri Ghat Road", 11.5516, 76.0412)
        addNode("kalpetta", "Kalpetta Town", 11.6094, 76.0815)
        addNode("sulthan_bathery", "Sulthan Bathery Junction", 11.6616, 76.2573)
        addNode("mananthavady", "Mananthavady GMC Junction", 11.7011, 76.1154)

        addBidirectionalEdge("kozhikode", "thamarassery", "NH 766 Wayanad Road")
        addBidirectionalEdge("thamarassery", "vythiri", "NH 766 Thamarassery Churam Pass")
        addBidirectionalEdge("vythiri", "kalpetta", "NH 766")
        addBidirectionalEdge("kalpetta", "sulthan_bathery", "NH 766")
        addBidirectionalEdge("kalpetta", "mananthavady", "Kalpetta-Mananthavady Road")

        // --- Malappuram & Palakkad Valley ---
        addNode("malappuram", "Malappuram Downhill", 11.0476, 76.0754)
        addNode("manjeri", "Manjeri GMC", 11.1215, 76.1265)
        addNode("perinthalmanna", "Perinthalmanna Highway", 10.9782, 76.2283)
        addNode("nilambur", "Nilambur Town", 11.2789, 76.2291)
        addNode("ottapalam", "Ottapalam Town", 10.7799, 76.3748)

        addBidirectionalEdge("ramanattukara", "malappuram", "Calicut-Malappuram Road")
        addBidirectionalEdge("malappuram", "manjeri", "Manjeri Road")
        addBidirectionalEdge("manjeri", "nilambur", "Nilambur Highway")
        addBidirectionalEdge("malappuram", "perinthalmanna", "SH 39")
        addBidirectionalEdge("perinthalmanna", "ottapalam", "Pattambi Road")
        addBidirectionalEdge("ottapalam", "palakkad", "Palakkad-Ponnani Road")

        // --- Idukki High Range (NH 85) ---
        addNode("kothamangalam", "Kothamangalam Town", 10.0630, 76.6340)
        addNode("adimali", "Adimali Junction", 10.0160, 76.9567)
        addNode("thodupuzha", "Thodupuzha Town", 9.8974, 76.7161)
        addNode("idukki_painavu", "Idukki Painavu GMC", 9.8510, 76.9635)

        addBidirectionalEdge("muvattupuzha", "kothamangalam", "NH 85")
        addBidirectionalEdge("kothamangalam", "adimali", "NH 85 Kochi-Dhanushkodi")
        addBidirectionalEdge("adimali", "idukki_painavu", "Adimali-Painavu Road")
        addBidirectionalEdge("muvattupuzha", "thodupuzha", "SH 8")
        addBidirectionalEdge("thodupuzha", "idukki_painavu", "Thodupuzha-Idukki Road")
    }

    private fun findClosestNode(lat: Double, lng: Double): RoadNode {
        var closest = nodes.values.first()
        var minDistance = Double.MAX_VALUE

        for (node in nodes.values) {
            val dist = computeDistanceKm(lat, lng, node.latitude, node.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closest = node
            }
        }
        return closest
    }

    fun findEmergencyRoute(
        startLat: Double,
        startLng: Double,
        destination: Hospital
    ): EmergencyRoute {
        val startNode = findClosestNode(startLat, startLng)
        val destNode = findClosestNode(destination.latitude, destination.longitude)

        val distances = mutableMapOf<String, Double>()
        val previousNodes = mutableMapOf<String, String>()
        val previousRoads = mutableMapOf<String, String>()

        nodes.keys.forEach { distances[it] = Double.MAX_VALUE }
        distances[startNode.id] = 0.0

        val queue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        queue.add(Pair(startNode.id, 0.0))

        while (queue.isNotEmpty()) {
            val (currentId, currentDist) = queue.poll()!!

            if (currentId == destNode.id) break
            if (currentDist > (distances[currentId] ?: Double.MAX_VALUE)) continue

            adjacencyList[currentId]?.forEach { edge ->
                val neighborId = edge.target.id
                val newDist = currentDist + edge.distanceKm
                if (newDist < (distances[neighborId] ?: Double.MAX_VALUE)) {
                    distances[neighborId] = newDist
                    previousNodes[neighborId] = currentId
                    previousRoads[neighborId] = edge.roadName
                    queue.add(Pair(neighborId, newDist))
                }
            }
        }

        // Reconstruct path
        val pathNodes = mutableListOf<RoadNode>()
        var curr: String? = destNode.id
        while (curr != null) {
            nodes[curr]?.let { pathNodes.add(0, it) }
            curr = previousNodes[curr]
            if (curr == startNode.id) {
                nodes[startNode.id]?.let { pathNodes.add(0, it) }
                break
            }
        }

        if (pathNodes.isEmpty() || pathNodes.first().id != startNode.id) {
            pathNodes.clear()
            pathNodes.add(startNode)
            if (startNode.id != destNode.id) {
                pathNodes.add(destNode)
            }
        }

        // GeoPoints polyline
        val polylinePoints = mutableListOf<GeoPoint>()
        polylinePoints.add(GeoPoint(startLat, startLng))
        for (node in pathNodes) {
            polylinePoints.add(node.toGeoPoint())
        }
        polylinePoints.add(GeoPoint(destination.latitude, destination.longitude))

        // Initial leg to road network
        val initialDistKm = computeDistanceKm(startLat, startLng, startNode.latitude, startNode.longitude)

        // Final leg from road network to hospital
        val finalDistKm = computeDistanceKm(destNode.latitude, destNode.longitude, destination.latitude, destination.longitude)

        // Build navigation instructions
        val steps = mutableListOf<RouteStep>()
        steps.add(
            RouteStep(
                instruction = "Depart current location towards arterial road: ${startNode.name}",
                distanceKm = initialDistKm
            )
        )

        for (i in 0 until pathNodes.size - 1) {
            val from = pathNodes[i]
            val to = pathNodes[i + 1]
            val road = adjacencyList[from.id]?.find { it.target.id == to.id }
            val roadName = road?.roadName ?: "Emergency Corridor"
            val dist = road?.distanceKm ?: 0.0

            steps.add(
                RouteStep(
                    instruction = "Follow $roadName towards ${to.name}",
                    distanceKm = dist
                )
            )
        }

        steps.add(
            RouteStep(
                instruction = "Turn into ${destination.name} (${destination.address})",
                distanceKm = finalDistKm
            )
        )

        val totalDistKm = initialDistKm + (distances[destNode.id]?.takeIf { it < Double.MAX_VALUE } ?: 0.0) + finalDistKm
        val estimatedMins = ((totalDistKm / 45.0) * 60).toInt().coerceAtLeast(1)

        return EmergencyRoute(
            destinationName = destination.name,
            destinationDistrict = destination.district,
            points = polylinePoints,
            totalDistanceKm = totalDistKm,
            estimatedMinutes = estimatedMins,
            steps = steps
        )
    }
}
