import time

pending_session_jobs = []


def process_session_job(data, fut):
    result = data["session"].handle(data["msg"], data["request"])
    fut["result"] = result
    fut["resolved"] = True


def process_pending_session_jobs():
    while len(pending_session_jobs) > 0:
        work_item = pending_session_jobs.pop(0)
        process_session_job(work_item["data"], work_item["fut"])


def request_session_job(data):
    fut = {"resolved": False}
    pending_session_jobs.append({"data": data,
                                 "fut": fut})
    return fut


def handle_session_message(session, msg, request):
    fut = request_session_job({"session": session,
                               "msg": msg,
                               "request": request})

    # TODO: is there a better way how to wait for work to be completed on other thread?
    while fut["resolved"] is False:
        time.sleep(0.03)

    return fut["result"]
