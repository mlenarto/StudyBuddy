/* The purpose of this function is to send a push notification to users that are within
 * a certain location and are enrolled in the course that the help request was made for.
 *
 * The user that made the request is does not recieve the push notification.
 *
 * This function is called when the duration is decremented by the background job,
 * so we check to see if the help request already exists before sending a push notification.
*/

Parse.Cloud.beforeSave("HelpRequest", function(request, response) {
	
    // Determine whether the help request is new or is being updated by the background job.
	var requestQuery = new Parse.Query("HelpRequest");
    /* objectId for the request object is undefined so we use the title, description, locationDescription, and geoLocation to ensure we have a "unique" match.
     */
	requestQuery.equalTo("title", request.object.get("title"));
	console.error("request title: " + request.object.get("title"));
	requestQuery.equalTo("description", request.object.get("description"));
	console.error("request description: " + request.object.get("description"));
    requestQuery.equalTo("locationDescription", request.object.get("locationDescription"));
    console.error("request location desc: " + request.object.get("locationDescription"));
    requestQuery.equalTo("geoLocation", request.object.get("geoLocation"));
    console.error("request geoLocation: " + request.object.get("geoLocation"));
	requestQuery.find({
		success: function(results) {
			if (results.length > 0)
			{
				//Don't send a push, the help request is being updated by the background job.
				response.success();
				console.error("help request exists, don't send a push");
			}
			else
			{
				console.error("help request does NOT exist, send a push");
				var MAX_DISTANCE = 50;	//miles
				var courseQuery = new Parse.Query("Course");
				var courseObj = request.object.get("course");
				courseQuery.equalTo("objectId", courseObj.id);
				courseQuery.find({
					success: function(results) {
						// Should only be one result (if not, still retrieve first element)
						var object = results[0];
						var courseNumber = object.get("courseNumber");
						var spacelessNum = courseNumber.replace(/\s+/g,'');
						
						// Find object where the array in channels contains spacelessNum
						var query = new Parse.Query(Parse.Installation);
						query.equalTo("channels", spacelessNum);

						// Don't send the push notification to the user that created it
						query.notEqualTo("user", Parse.User.current());
					//	query.whereWithin("geoLocation", object.get("geoLocation"), MAX_DISTANCE);	
						// Send push notifications to users following spacelessNum channel
						Parse.Push.send({
							where: query,
							data: { alert: "Someone in " + courseNumber + " needs your help!" }
						},
						{
							success: function() {
								//Push was successful
								response.success();
							},
							error: function(error) {
							console.error("Got an error " + error.code + " : " + error.message);
							}
						});
					},
					error: function() {
						response.error("error finding course");
					}
				});

			}
		},
		error: function() {
			response.error("error querying for help request");
		}
	});

});

// Delete help requests when their timer runs out. Executed every one minute.
Parse.Cloud.job("requestTimer", function(request, status) {
	var REPEAT_TIME = 60000;	//one minute
	var HelpRequests = Parse.Object.extend("HelpRequest");
	var query = new Parse.Query(HelpRequests);
	query.ascending("duration");
	query.find({
		success: function(requestList) {
			// request list was retrieved successfully
			for (var i = 0; i < requestList.length; i++)
			{
				var new_duration = requestList[i].get("duration") - REPEAT_TIME;
				if (new_duration <= 0)
				{
					requestList[i].destroy({
						success: function(myObject) {
							console.error("Sucessfully deleted request");
						},
						error: function(myObject, error) {
							console.error("Error when deleting request");
						}
					});
				}
				else
				{
					requestList[i].set("duration", new_duration);
					requestList[i].save();
				}
			}
	},
	error: function(object, error) {
		// the object was not retrieved successfully
		console.error("\nWas not able to retrieve deltaList");
	}
	}).then(function() {
		// Set the job's success status
		status.success("Background job completed successfully.");
	}, function(object, error) {
		// Set the job's error status
		status.error("Uh oh, something went wrong.");
	});
});
