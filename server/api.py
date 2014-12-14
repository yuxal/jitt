import webapp2
import jinja2
import os
import logging
import json
import datetime
import md5
import itertools

from random import random
from collections import Counter
from google.appengine.ext import ndb
from google.appengine.api import urlfetch

class Suggestion(ndb.Model):
    app_id = ndb.StringProperty()
    str_key = ndb.StringProperty()
    translated = ndb.StringProperty()
    locale = ndb.StringProperty()
    votes = ndb.IntegerProperty()

class UserAction(ndb.Model):
    deviceid = ndb.StringProperty()
    action = ndb.StringProperty()
    suggestion = ndb.KeyProperty(kind="Suggestion")

def get_action(deviceid,suggestion):
    actions = UserAction.query(UserAction.deviceid==deviceid,UserAction.suggestion==suggestion.key).fetch(1)
    if len(actions) > 0:
        return actions[0].action
    else:
        return "none"

class GetTranslations(webapp2.RequestHandler):
    def get(self):
        app_id = self.request.get('app_id')
        device_id = self.request.get('device_id')
        keys = self.request.get_all('key')
        translated_locales = [ x.split('_')[0] for x in self.request.get_all('locale') ]
        logging.error("locales %r" % translated_locales)

        suggestions = Suggestion.query(Suggestion.app_id==app_id, Suggestion.str_key.IN(keys), Suggestion.locale.IN(translated_locales)).order(-Suggestion.votes).fetch(1000)
        logging.error("suggestions %d, %r" % (len(suggestions),suggestions))
        ret = { k: {l:[] for l in translated_locales} for k in keys}
        for s in suggestions:
            ret.setdefault(s.str_key,{l:[] for l in translated_locales})[s.locale].append({ 'suggested':s.translated, 'votes':s.votes, 'user_selected':get_action(device_id,s) })

        # ret = { key: { lang: [ { 'suggested' : s.translated, 'votes' : s.votes, 'user_selected' : get_action(device_id,s) } for s in strings]
        #                         for lang, strings in itertools.groupby(suggestions.get(key,[]),lambda x:x.locale) }
        #                 for key in keys }

        self.response.write(json.dumps(ret))

class DoAction(webapp2.RequestHandler):
    def post(self):
        app_id = self.request.get('app_id')
        device_id = self.request.get('device_id')
        locale = self.request.get('locale').split('_')[0]
        key = self.request.get('key')
        string = self.request.get('string')
        action = self.request.get('action')
        suggestion = Suggestion.query(Suggestion.app_id==app_id,Suggestion.str_key==key,Suggestion.translated==string,Suggestion.locale==locale).fetch(1)
        if len(suggestion)>0:
            suggestion = suggestion[0]
        else:
            action = "new"
            suggestion = Suggestion(app_id=app_id,str_key=key,translated=string,locale=locale,votes=1)
            suggestion.put()
        user_action = UserAction.query(UserAction.deviceid==device_id,UserAction.suggestion==suggestion.key).fetch(1)
        if len(user_action) > 0:
            user_action = user_action[0]
        else:
            user_action = UserAction(deviceid=device_id, suggestion=suggestion.key, action="xxx")
        orig_votes = suggestion.votes
        orig_action = user_action.action

        if user_action.action != "new":
            if user_action.action != action:
                if user_action.action == "up":     suggestion.votes -= 1
                elif user_action.action == "down": suggestion.votes += 1
                user_action.action = action
                if user_action.action == "up":     suggestion.votes += 1
                elif user_action.action == "down": suggestion.votes -= 1

                ad = True
        if orig_action != user_action.action:
            user_action.put()
        if orig_votes != suggestion.votes:
            suggestion.put()

        self.response.write("OK")

application = webapp2.WSGIApplication([
    ('/api/translations', GetTranslations),
    ('/api/action', DoAction),
], debug=True)
