import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

replacement = """        // Desenha o círculo da Cerca Virtual se estiver ativada
        if (window.geofenceEnabled) {
            window.drawGeofenceCircleOnMap();
        }
        
        // --- INTEGRAÇÃO MAPS API: DESENHAR ROTA NO PAINEL PRINCIPAL ---
        let actOrder = window.mergedActiveOrder;
        if (!actOrder && typeof activeOrderId !== 'undefined' && activeOrderId && typeof currentOrdersList !== 'undefined') {
            actOrder = currentOrdersList.find(o => o.id === activeOrderId && o.status !== 'canceled');
        }
        if (actOrder && typeof google !== 'undefined' && google.maps && google.maps.DirectionsService) {
           const dirService = new google.maps.DirectionsService();
           const dirRenderer = new google.maps.DirectionsRenderer({
               map: window.trafficMapInstance,
               suppressMarkers: false,
               polylineOptions: { strokeColor: "#3a86ff", strokeWeight: 6, strokeOpacity: 0.8 }
           });
           
           if (actOrder.isMerged) {
               const pickup1 = actOrder.pickup_address;
               const pickup2 = actOrder.pickup_address_2;
               const delivery1 = actOrder.delivery_address;
               const delivery2 = actOrder.delivery_address_2;
               dirService.route({
                   origin: pickup1,
                   destination: delivery2,
                   waypoints: [
                       { location: pickup2, stopover: true },
                       { location: delivery1, stopover: true }
                   ],
                   optimizeWaypoints: false,
                   travelMode: google.maps.TravelMode.DRIVING
               }, (res, status) => {
                   if (status === 'OK') {
                       dirRenderer.setDirections(res);
                   }
               });
           } else {
               dirService.route({
                   origin: actOrder.pickup_address,
                   destination: actOrder.delivery_address,
                   travelMode: google.maps.TravelMode.DRIVING
               }, (res, status) => {
                   if (status === 'OK') {
                       dirRenderer.setDirections(res);
                   }
               });
           }
        }
        // --------------------------------------------------------------
"""

content = content.replace('        // Desenha o círculo da Cerca Virtual se estiver ativada\n        if (window.geofenceEnabled) {\n            window.drawGeofenceCircleOnMap();\n        }', replacement)

with open(file_path, 'w') as f:
    f.write(content)

