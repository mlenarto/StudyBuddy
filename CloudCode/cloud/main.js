// After a new help request is created, this function will execute
Parse.Cloud.afterSave("HelpRequest", function(request) {
	
	var courseQuery = new Parse.Query("Course");
	var courseObj = request.object.get("course");
	courseQuery.equalTo("objectId", courseObj.id);
	courseQuery.find({
		success: function(results) {
	
			// Should only be one result (if not, still retrieve first element)
			var object = results[0];

			// Retrieve course number from Course object
			var courseNumber = object.get("courseNumber");

			// Remove spaces so we can perform a channel search
			var spacelessNum = courseNumber.replace(/\s+/g,'');
			
			// Find object where the array in channels contains spacelessNum
			var query = new Parse.Query(Parse.Installation);
			query.equalTo('channels', spacelessNum);

			// Don't send the push notification to the user that created it
			query.notEqualTo('user', Parse.User.current());
			
			// Send push notifications to users following spacelessNum channel
			Parse.Push.send({
				where: query,
				data: { alert: "Someone in " + courseNumber + " needs your help!" }
			},
			{
				success: function() {
				//Push was successful
				},
    				error: function(error) {
      				console.error("Got an error " + error.code + " : " + error.message);
    				}
  			});
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
