/**
 * Go back, or somewhere sensible when there is no back to go to.
 *
 * router.back() is a pop, not a destination. It only works when this screen was pushed
 * onto something. Arrive by deep link instead -- a typed URL, a refreshed browser tab,
 * a shared link -- and the stack beneath is empty, so the pop silently does nothing and
 * the screen reads as frozen with no way out. That is easy to miss on a phone, where
 * you rarely arrive anywhere except by tapping, and immediate on the web client.
 *
 * `fallback` is where the screen belongs when there is no history: the tab whose list
 * would have led here. replace() rather than push(), so the dead-end screen does not
 * stay underneath waiting to be popped back to.
 */
export function goBack(router, fallback) {
    if (router.canGoBack()) {
        router.back();
        return;
    }
    router.replace(fallback);
}
