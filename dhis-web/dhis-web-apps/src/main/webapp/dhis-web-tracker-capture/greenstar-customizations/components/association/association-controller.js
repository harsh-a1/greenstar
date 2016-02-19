
trackerCapture.controller('EventToTEIAssociations',
    function ($rootScope,
              $scope,
              $modal,
              $timeout,
              AjaxCalls,
			  ModalService,
              utilityService,
              DHIS2EventFactory) {

        $scope.teiAttributesMap = [];
        $scope.trackedEntityMap = [];

        var contextMenu =new contextMenoo([{
            text:"Delete",
            id:"oo-delete",
            icon:"greenstar-customizations/resources/img/icons/delete.png"
        }]);

        $scope.$on('association-widget', function refresh(event, args) {

            $scope.TEtoEventTEIMap = [];
            $scope.TEWiseEventTEIs = [];
            if (args.show){
               $scope.eventSelected = true
                AjaxCalls.getEventbyId(args.event.event).then(function(event){
                    $scope.selectedEvent = event;

                    if (!event.eventMembers){ // this will add association to event if none are associated
                        // get all events of this TEI and push the event members of the latest event into this event
                        //AjaxCalls.getAllEventsByTEI(event.trackedEntityInstance).then(function(data){
                        //
                        //    var allEventMembers = extractAllEventMembers(data.events);
                        //    if (allEventMembers.length == 0)
                        //    return
                        //
                        //    $scope.selectedEvent.eventMembers = allEventMembers;
                        //    updateEvent($scope.selectedEvent);
                        //    refresh($scope.selectedEvent,{event : $scope.selectedEvent , show :true});

                            //Old logic of getting associations of latest event
                            //sort by event date and exclude scheduled ones
                            //var latestEvent = getLatestActiveEvent(data.events,event);
                            //if (latestEvent)
                            //    AjaxCalls.getEventbyId(latestEvent.event).then(function(latestEventFull){
                            //        if (latestEventFull.eventMembers){
                            //            $scope.selectedEvent.eventMembers = latestEventFull.eventMembers;
                            //            updateEvent($scope.selectedEvent);
                            //            refresh($scope.selectedEvent,{event : $scope.selectedEvent , show :true});
                            //        }
                            //    })
                            //Old logic ends
                        //})
                    }else
                    for (var i=0;i<event.eventMembers.length;i++){
                        if (!$scope.TEtoEventTEIMap[event.eventMembers[i].trackedEntity]){
                            $scope.TEtoEventTEIMap[event.eventMembers[i].trackedEntity] = [];
                        }
                            $scope.TEtoEventTEIMap[event.eventMembers[i].trackedEntity].push(event.eventMembers[i]);
                    }


                    for (key in $scope.TEtoEventTEIMap){
                        var TEIList = [];
                        for (var j=0;j<$scope.TEtoEventTEIMap[key].length;j++) {
                            updateMap($scope.TEtoEventTEIMap[key][j]);
                            TEIList.push($scope.TEtoEventTEIMap[key][j])
                        }
                        $scope.TEWiseEventTEIs.push({
                            id: key,
                            trackedEntity: $scope.trackedEntityMap[key].displayName,
                            TEIList :TEIList});
                    }
                })
            }else {
                $scope.selectedEvent = undefined;
            }
        });

        // get all no program attributes
        //AjaxCalls.getNoProgramAttributes().then(function(data){
        //    $scope.noProgramAttributes = data.trackedEntityAttributes;
        //})

        //get attributes for display in association widget
        AjaxCalls.getAssociationWidgetAttributes().then(function(associationWidgetAttributes){
            $scope.associationWidgetAttributes = associationWidgetAttributes;
        })

        // get all tracked entities
        AjaxCalls.getTrackedEntities().then(function(data){
            if (data.trackedEntities)
            $scope.trackedEntityMap = utilityService.prepareIdToObjectMap(data.trackedEntities,"id");
        })

        $scope.showHomeScreen = function () {
            var modalInstance = $modal.open({
                templateUrl: 'greenstar-customizations/components/association/addAssociation.html',
                controller: 'AddAssociationController',
                windowClass: 'modal-full-window',
                resolve: {

                }
            });
            modalInstance.selectedEvent = $scope.selectedEvent;
            modalInstance.result.then(function () {

            }, function () {
            });
        }

        getLatestActiveEvent = function(events,selectedEvent){
        var latestEvent = undefined;
            for (var i=0; i< events.length; i++){

                if (events[i].status == "SCHEDULE")
                continue

                if (events[i].event == selectedEvent.event)
                continue

                if (!latestEvent){
                    latestEvent = events[i];
                }else{
                    if (latestEvent.eventDate < events[i].eventDate){
                        latestEvent = events[i];
                    }
                }
            }
            return latestEvent;
        }

        updateEvent = function(event){
            DHIS2EventFactory.update(event).then(function(response){
                if (response.httpStatus == "OK"){
                    $timeout(function () {
                        $rootScope.$broadcast('association-widget', {event : event, show :true});
                    }, 200);
                }else{
                    alert("An unexpected thing occurred.");
                }
            })
        }

        updateMap = function(tei){

            for (var i=0;i<tei.attributes.length;i++){

                if (!$scope.teiAttributesMap[tei.trackedEntityInstance]){
                    $scope.teiAttributesMap[tei.trackedEntityInstance] = []
                }
                $scope.teiAttributesMap[tei.trackedEntityInstance][tei.attributes[i].attribute] = tei.attributes[i].value;
            }
        }

       deleteAssociation = function(teiId,event){

            var modalOptions = {
                closeButtonText: 'cancel',
                actionButtonText: 'delete',
                headerText: 'delete',
                bodyText: 'are_you_sure_to_delete'
            };

            ModalService.showModal({}, modalOptions).then(function(result){

                if (event.eventMembers.length)
                {
                    for ( var i=0;i<event.eventMembers.length;i++ )
                    {
                        if (event.eventMembers[i].trackedEntityInstance == teiId)
                        { 
                            event.eventMembers.splice(i,1);
                        }
                    }
                    if (event.eventMembers.length == 0)
                    {
                        delete(event.eventMembers);
                    }
                }

                updateEvent(event);

                //update events list after delete tei

                DHIS2EventFactory.update(event).then(function(response)
                {
                    if (response.httpStatus == "OK")
                    {
                        $timeout(function () {
                            $rootScope.$broadcast('invitation-div', {event : event, show :true});
                        }, 200);
                    }
                    else
                    {
                        alert("An unexpected thing occurred.");
                    }
                });

            });

        }
        $scope.showContextMenu = function(event,tei,selectedEvent){

             contextMenu.draw(event);
           // $('#oo-delete').on('click', function(e){
                contextMenu.finish();
                deleteAssociation(tei.trackedEntityInstance,selectedEvent);
                console.log('clicked', this);
           // })
        }
    });
