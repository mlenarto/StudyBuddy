Parse.Cloud.afterSave("HelpRequest", function(request) {
	
	var query = new Parse.Query(Parse.Installation);
	query.equalTo('channels', request.object.get("course").title);

	Parse.Push.send({
		where: query,
		data: { alert: "Someone in " + course.title + " needs your help!" }
	},
	{
		success: function() {
		//Push was successful
		},
    		error: function(error) {
      		console.error("Got an error " + error.code + " : " + error.message);
    		}
  	});
});
