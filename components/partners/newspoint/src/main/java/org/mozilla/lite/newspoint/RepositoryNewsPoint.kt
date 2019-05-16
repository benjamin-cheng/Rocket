package org.mozilla.lite.newspoint

import android.content.Context
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import org.mozilla.lite.partner.Repository
import org.mozilla.lite.partner.Repository.Parser
import java.util.Locale

@ImplicitReflectionSerializer
class RepositoryNewsPoint(context: Context, subscriptionUrl: String) : Repository<NewsPointItem>(context, null, 3, null, null, SUBSCRIPTION_KEY_NAME, subscriptionUrl, FIRST_PAGE, PARSER, true) {

    override fun getSubscriptionUrl(pageNumber: Int): String {
        return String.format(Locale.US, subscriptionUrl, pageNumber, DEFAULT_PAGE_SIZE)
    }

    companion object {
        internal const val SUBSCRIPTION_KEY_NAME = "newspoint"
        internal const val FIRST_PAGE = 1

        @UnstableDefault
        @ImplicitReflectionSerializer
        internal var PARSER: Parser<NewsPointItem> = Parser { source ->
            Json.nonstrict.parse<NewsPointItemHolder>(source).items
        }

//            source ->
//            var ret = ArrayList<NewsPointItem>()
//            ret = Json.parse(NewsPointItem.serializer(), source)
//                // TODO: 11/2/18 It takes 0.1s - 0.2s to create JsonObject, do we want to improve this?
//                JSONObject root = new JSONObject(source);
//                JSONArray items = root.getJSONArray("items");
//                for (int i = 0 ; i < items.length() ; i++) {
//                    JSONObject row = items.getJSONObject(i);
//                    String id = safeGetString(row, "id");
//                    String hl = safeGetString(row, "hl");
//                    String imageid  = safeGetString(row, "imageid");
//                    JSONArray array = safeGetArray(row, "images");
//                    String imageUrl = array == null ? null : array.getString(0);
//                    String pn  = safeGetString(row, "pn");
//                    String dl  = safeGetString(row, "dl");
//                    String dm  = safeGetString(row, "dm");
//                    long pid  = safeGetLong(row, "pid");
//                    long lid  = safeGetLong(row, "lid");
//                    String lang  = safeGetString(row, "lang");
//                    String tn  = safeGetString(row, "tn");
//                    String wu  = safeGetString(row, "wu");
//                    String pnu  = safeGetString(row, "pnu");
//                    String fu  = safeGetString(row, "fu");
//                    String sec  = safeGetString(row, "sec");
//                    String mwu  = safeGetString(row, "mwu");
//                    String m  = safeGetString(row, "m");
//                    String separator = "" + '\0';
//                    List<String> tags = Arrays.asList(row.getJSONArray("tags").join(separator).split(separator));
//                    if (id == null || hl == null || mwu == null || dl == null) {
//                        continue;
//                    }
//                    long timestamp = 0;
//                    try {
//                        timestamp = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'IST' yyyy", Locale.US).parse(dl).getTime();
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                        // skip this item
//                        continue;
//                    }
//                    NewsPointItem newspointeItem = new NewsPointItem(id, imageUrl, hl, mwu, timestamp, imageid, pn, dm, pid, lid, lang, tn, wu, pnu, fu, sec, m, tags);
//                    ret.add(newspointeItem);
//                }
//            ret
//        }
//
//        private fun safeGetArray(`object`: JSONObject, key: String): JSONArray? {
//            return try {
//                `object`.getJSONArray(key)
//            } catch (ex: JSONException) {
//                null
//            }
//
//        }
//
//        private fun safeGetString(`object`: JSONObject, key: String): String? {
//            return try {
//                `object`.getString(key)
//            } catch (ex: JSONException) {
//                null
//            }
//
//        }
//
//        private fun safeGetLong(`object`: JSONObject, key: String): Long {
//            return try {
//                `object`.getLong(key)
//            } catch (ex: JSONException) {
//                -1L
//            }
//
//        }
    }
}
