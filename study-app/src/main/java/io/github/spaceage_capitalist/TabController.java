package io.github.spaceage_capitalist;

import java.util.function.Consumer;

public interface TabController {

    /** Gets the title of the tab */
    public String getTitle();

    /** Focuses the tab */
    public void focusTab();

    /** Force closes the tab */
    public void closeTab();

    /** Requests for the tab to close, allowing the tab to decide whether to close, prompt the user, or ignore the request.
     *  @return True if the tab was closed, false otherwise.
    */
    public boolean closeTabRequest();

    /**
     * Prompts the tab to act as if it were being requested to close and returns whether it would have closed or not without actually closing the tab.
     * This is useful for bulk closing tabs where it is important to make sure every tab will close before closing any.
     * @return True if the tab is ready to be closed, false otherwise.
     */
    public boolean getCloseConsent();
}